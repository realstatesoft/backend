package com.openroof.openroof.service;

import com.openroof.openroof.config.ReservationProperties;
import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.dto.reservation.CancelReservationRequest;
import com.openroof.openroof.dto.reservation.CreateReservationRequest;
import com.openroof.openroof.dto.reservation.ReservationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.NotificationType;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.ReservationStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.reservation.Reservation;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.ReservationRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.PropertySecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final EnumSet<ReservationStatus> BLOCKING_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.ACTIVE);

    private static final EnumSet<ReservationStatus> NON_CANCELLABLE_STATUSES =
            EnumSet.of(ReservationStatus.CANCELLED, ReservationStatus.EXPIRED, ReservationStatus.CONVERTED_TO_CONTRACT);

    private final ReservationRepository reservationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertySecurity propertySecurity;
    private final NotificationService notificationService;
    private final ReservationProperties properties;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest req, String currentUserEmail) {
        User buyer = getUserByEmail(currentUserEmail);
        Property property = propertyRepository.findById(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada: " + req.propertyId()));

        if (property.getStatus() != PropertyStatus.PUBLISHED) {
            throw new BadRequestException("Solo se puede reservar una propiedad publicada");
        }
        if (property.getOwner().getId().equals(buyer.getId())) {
            throw new BadRequestException("No puedes reservar tu propia propiedad");
        }
        if (reservationRepository.existsBlockingReservation(property.getId(), BLOCKING_STATUSES)) {
            throw new BadRequestException("Esta propiedad ya tiene una reserva activa");
        }

        Reservation reservation = Reservation.builder()
                .property(property)
                .buyer(buyer)
                .reservationAmount(req.amount())
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(properties.ttlHours()))
                .notes(req.notes())
                .build();

        Reservation saved;
        try {
saved = reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Esta propiedad ya tiene una reserva activa");
        }

        notifyOwner(saved, "Nueva reserva recibida",
                String.format("%s reservó tu propiedad '%s'.", buyer.getName(), property.getTitle()));

        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse confirmReservation(Long id, String currentUserEmail) {
        Reservation reservation = getReservationOrThrow(id);
        User currentUser = getUserByEmail(currentUserEmail);

        if (!propertySecurity.canModify(reservation.getProperty().getId(), currentUser)) {
            throw new ForbiddenException("No tienes permiso para confirmar esta reserva");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Solo se pueden confirmar reservas pendientes");
        }
        if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("No se pueden confirmar reservas vencidas");
        }

        reservation.setStatus(ReservationStatus.ACTIVE);
        Reservation saved = reservationRepository.saveAndFlush(reservation);

        notifyBuyer(saved, "Reserva confirmada",
                String.format("Tu reserva sobre '%s' fue confirmada.", reservation.getProperty().getTitle()));

        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id, CancelReservationRequest req, String currentUserEmail) {
        Reservation reservation = getReservationOrThrow(id);
        User currentUser = getUserByEmail(currentUserEmail);

        boolean isBuyer = reservation.getBuyer().getId().equals(currentUser.getId());
        boolean canManage = propertySecurity.canModify(reservation.getProperty().getId(), currentUser);
        if (!isBuyer && !canManage) {
            throw new ForbiddenException("No tienes permiso para cancelar esta reserva");
        }
        if (NON_CANCELLABLE_STATUSES.contains(reservation.getStatus())) {
            throw new BadRequestException(
                    "La reserva no se puede cancelar en estado: " + reservation.getStatus().name());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledReason(req.reason());
        Reservation saved = reservationRepository.saveAndFlush(reservation);

        if (!isBuyer) {
            notifyBuyer(saved, "Reserva cancelada",
                    String.format("Tu reserva sobre '%s' fue cancelada.", reservation.getProperty().getTitle()));
        }
        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse convertToContract(Long id, String currentUserEmail) {
        Reservation reservation = getReservationOrThrow(id);
        User currentUser = getUserByEmail(currentUserEmail);

        if (!propertySecurity.canModify(reservation.getProperty().getId(), currentUser)) {
            throw new ForbiddenException("No tienes permiso sobre esta reserva");
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BadRequestException("Solo reservas ACTIVE se pueden convertir a contrato");
        }

        reservation.setStatus(ReservationStatus.CONVERTED_TO_CONTRACT);
        Reservation saved = reservationRepository.saveAndFlush(reservation);
        return toResponse(saved);
    }

    @Transactional
    @Scheduled(cron = "0 */10 * * * *")
    @SchedulerLock(lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public int expireStaleReservations() {
        List<Reservation> stale = reservationRepository.findExpired(BLOCKING_STATUSES, LocalDateTime.now());
        if (stale.isEmpty()) return 0;
        stale.forEach(r -> r.setStatus(ReservationStatus.EXPIRED));
        reservationRepository.saveAll(stale);

        List<Long> ids = stale.stream().map(Reservation::getId).toList();
        List<String> titles = stale.stream().map(r -> r.getProperty().getTitle()).toList();
        List<String> buyerEmails = stale.stream().map(r -> r.getBuyer().getEmail()).toList();
        List<Long> buyerIds = stale.stream().map(r -> r.getBuyer().getId()).toList();

        afterCommit(() -> {
            for (int i = 0; i < ids.size(); i++) {
                notificationService.create(
                        new CreateNotificationRequest(
                                buyerIds.get(i),
                                NotificationType.RESERVATION,
                                "Reserva expirada",
                                String.format("Tu reserva sobre '%s' expiró.", titles.get(i)),
                                Map.of("reservationId", ids.get(i)),
                                "/reservations"
                        ),
                        buyerEmails.get(i)
                );
            }
            log.info("Expired {} stale reservations", ids.size());
        });
        return stale.size();
    }

    public ReservationResponse getById(Long id, String currentUserEmail) {
        Reservation reservation = getReservationOrThrow(id);
        User currentUser = getUserByEmail(currentUserEmail);

        boolean isBuyer = reservation.getBuyer().getId().equals(currentUser.getId());
        boolean canManage = propertySecurity.canModify(reservation.getProperty().getId(), currentUser);
        if (!isBuyer && !canManage) {
            throw new ForbiddenException("No tienes permiso para ver esta reserva");
        }
        return toResponse(reservation);
    }

    public Page<ReservationResponse> getMyReservations(String currentUserEmail, ReservationStatus status, Pageable pageable) {
        User buyer = getUserByEmail(currentUserEmail);
        return reservationRepository.findByBuyerFiltered(buyer.getId(), status, pageable)
                .map(this::toResponse);
    }

    public List<ReservationResponse> getByProperty(Long propertyId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        if (!propertySecurity.canModify(propertyId, currentUser)) {
            throw new ForbiddenException("No tienes permiso para ver reservas de esta propiedad");
        }
        return reservationRepository.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toResponse).toList();
    }

    public Optional<ReservationResponse> getMyReservationForProperty(Long propertyId, String currentUserEmail) {
        User buyer = getUserByEmail(currentUserEmail);
        return reservationRepository
                .findFirstByProperty_IdAndBuyer_IdAndStatusInOrderByCreatedAtDesc(
                        propertyId, buyer.getId(), BLOCKING_STATUSES)
                .map(this::toResponse);
    }

    public Page<ReservationResponse> getReservationsAsOwner(String currentUserEmail, ReservationStatus status, Pageable pageable) {
        User owner = getUserByEmail(currentUserEmail);
        return reservationRepository
                .findByPropertyOwnerFiltered(owner.getId(), status, pageable)
                .map(this::toResponse);
    }

    public Page<ReservationResponse> getReservationsAsAgent(String currentUserEmail, ReservationStatus status, Pageable pageable) {
        User agent = getUserByEmail(currentUserEmail);
        return reservationRepository
                .findByPropertyAgentUserFiltered(agent.getId(), status, pageable)
                .map(this::toResponse);
    }

    private Reservation getReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    private void notifyOwner(Reservation r, String title, String message) {
        notificationService.create(new CreateNotificationRequest(
                r.getProperty().getOwner().getId(),
                NotificationType.RESERVATION,
                title,
                message,
                Map.of("reservationId", r.getId(), "propertyId", r.getProperty().getId()),
                "/properties/" + r.getProperty().getId()
        ), r.getProperty().getOwner().getEmail());
    }

    private void notifyBuyer(Reservation r, String title, String message) {
        notificationService.create(new CreateNotificationRequest(
                r.getBuyer().getId(),
                NotificationType.RESERVATION,
                title,
                message,
                Map.of("reservationId", r.getId(), "propertyId", r.getProperty().getId()),
                "/reservations"
        ), r.getBuyer().getEmail());
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getProperty().getId(),
                r.getProperty().getTitle(),
                r.getBuyer().getId(),
                r.getBuyer().getName(),
                r.getBuyer().getEmail(),
                r.getReservationAmount(),
                r.getStatus(),
                r.getExpiresAt(),
                r.getNotes(),
                r.getCancelledReason(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}