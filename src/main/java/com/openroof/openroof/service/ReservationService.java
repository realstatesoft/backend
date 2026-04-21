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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Reservation saved = reservationRepository.save(reservation);

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

        reservation.setStatus(ReservationStatus.ACTIVE);
        Reservation saved = reservationRepository.save(reservation);

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
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.EXPIRED
                || reservation.getStatus() == ReservationStatus.CONVERTED_TO_CONTRACT) {
            throw new BadRequestException("La reserva ya no se puede cancelar en su estado actual");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledReason(req.reason());
        Reservation saved = reservationRepository.save(reservation);

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
        Reservation saved = reservationRepository.save(reservation);
        return toResponse(saved);
    }

    @Transactional
    @Scheduled(cron = "0 */10 * * * *")
    public int expireStaleReservations() {
        List<Reservation> stale = reservationRepository.findExpired(BLOCKING_STATUSES, LocalDateTime.now());
        if (stale.isEmpty()) return 0;
        stale.forEach(r -> r.setStatus(ReservationStatus.EXPIRED));
        reservationRepository.saveAll(stale);
        stale.forEach(r -> notifyBuyer(r, "Reserva expirada",
                String.format("Tu reserva sobre '%s' expiró.", r.getProperty().getTitle())));
        log.info("Expired {} stale reservations", stale.size());
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

    public Page<ReservationResponse> getMyReservations(String currentUserEmail, Pageable pageable) {
        User buyer = getUserByEmail(currentUserEmail);
        return reservationRepository.findByBuyer_IdOrderByCreatedAtDesc(buyer.getId(), pageable)
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

    public Page<ReservationResponse> getReservationsAsOwner(String currentUserEmail, Pageable pageable) {
        User owner = getUserByEmail(currentUserEmail);
        return reservationRepository
                .findByProperty_Owner_IdOrderByCreatedAtDesc(owner.getId(), pageable)
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
}