package com.openroof.openroof.service;

import com.openroof.openroof.dto.subscription.SubscriptionResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.subscription.Subscription;
import com.openroof.openroof.model.subscription.SubscriptionPlan;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.SubscriptionRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanService subscriptionPlanService;

    // ADMIN: listar todas las suscripciones con filtro opcional de estado
    public Page<SubscriptionResponse> getAll(SubscriptionStatus status, Pageable pageable) {
        if (status != null) {
            return subscriptionRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return subscriptionRepository.findAll(pageable).map(this::toResponse);
    }

    // ADMIN: ver una suscripción por ID
    public SubscriptionResponse getById(Long id) {
        return toResponse(getSubscriptionOrThrow(id));
    }

    // Usuario: historial de sus suscripciones
    public Page<SubscriptionResponse> getMySubscriptions(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return subscriptionRepository.findByUser_Id(user.getId(), pageable).map(this::toResponse);
    }

    // Usuario: suscripción activa actual
    public Optional<SubscriptionResponse> getMyActiveSubscription(String email) {
        User user = getUserByEmail(email);
        return subscriptionRepository
                .findFirstByUser_IdAndStatusOrderByExpiresAtDesc(user.getId(), SubscriptionStatus.ACTIVE)
                .map(this::toResponse);
    }

    // Llamado internamente desde PaymentService al aprobar un pago de tipo SUBSCRIPTION
    @Transactional
    public SubscriptionResponse activateSubscription(Long userId, Long paymentId, Long planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        SubscriptionPlan plan = subscriptionPlanService.getPlanOrThrow(planId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMonths(plan.getDurationMonths());

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .payment(payment)
                .startsAt(now)
                .expiresAt(expiresAt)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Suscripción activada: userId={}, planId={}, expiresAt={}", userId, planId, expiresAt);
        return toResponse(saved);
    }

    // Usuario puede cancelar la suya; ADMIN puede cancelar cualquiera
    @Transactional
    public SubscriptionResponse cancelSubscription(Long id, String email) {
        Subscription subscription = getSubscriptionOrThrow(id);
        User requestingUser = getUserByEmail(email);

        boolean isOwner = subscription.getUser().getId().equals(requestingUser.getId());
        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("No tienes permiso para cancelar esta suscripción");
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Solo se pueden cancelar suscripciones en estado ACTIVE");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());

        return toResponse(subscriptionRepository.save(subscription));
    }

    // Llamado por el scheduler para marcar como expiradas
    @Transactional
    public int deactivateExpired() {
        int count = subscriptionRepository.expireOlderThan(
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED,
                LocalDateTime.now()
        );
        if (count > 0) {
            log.info("Suscripciones expiradas marcadas: {}", count);
        }
        return count;
    }

    private Subscription getSubscriptionOrThrow(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getName(),
                subscriptionPlanService.toResponse(s.getPlan()),
                s.getStatus(),
                s.getPayment() != null ? s.getPayment().getId() : null,
                s.getStartsAt(),
                s.getExpiresAt(),
                s.getCancelledAt(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
