package com.openroof.openroof.service;

import com.openroof.openroof.dto.subscription.SubscriptionPlanRequest;
import com.openroof.openroof.dto.subscription.SubscriptionPlanResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ConflictException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.model.subscription.SubscriptionPlan;
import com.openroof.openroof.repository.SubscriptionPlanRepository;
import com.openroof.openroof.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Page<SubscriptionPlanResponse> getAll(Boolean active, Pageable pageable) {
        if (active != null) {
            return planRepository.findByActive(active, pageable).map(this::toResponse);
        }
        return planRepository.findAll(pageable).map(this::toResponse);
    }

    public SubscriptionPlanResponse getById(Long id) {
        return toResponse(getPlanOrThrow(id));
    }

    public SubscriptionPlanResponse getActiveById(Long id) {
        SubscriptionPlan plan = getPlanOrThrow(id);
        if (!plan.getActive()) {
            throw new ResourceNotFoundException("Plan de suscripción no encontrado");
        }
        return toResponse(plan);
    }

    @Transactional
    public SubscriptionPlanResponse create(SubscriptionPlanRequest request) {
        String trimmedName = request.name().trim();
        if (planRepository.existsByName(trimmedName)) {
            throw new ConflictException("Ya existe un plan con el nombre: " + trimmedName);
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(trimmedName)
                .description(request.description() != null ? request.description().trim() : null)
                .price(request.price())
                .durationMonths(request.durationMonths())
                .active(request.active() != null ? request.active() : true)
                .build();

        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public SubscriptionPlanResponse update(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = getPlanOrThrow(id);

        String trimmedName = request.name().trim();
        if (planRepository.existsByNameAndIdNot(trimmedName, id)) {
            throw new ConflictException("Ya existe un plan con el nombre: " + trimmedName);
        }

        plan.setName(trimmedName);
        plan.setDescription(request.description() != null ? request.description().trim() : null);
        plan.setPrice(request.price());
        plan.setDurationMonths(request.durationMonths());
        if (request.active() != null) {
            plan.setActive(request.active());
        }

        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public SubscriptionPlanResponse deactivate(Long id) {
        SubscriptionPlan plan = getPlanOrThrow(id);
        plan.setActive(false);
        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public void delete(Long id) {
        SubscriptionPlan plan = getPlanOrThrow(id);

        boolean hasActiveSubscriptions = subscriptionRepository.existsByPlan_IdAndStatusIn(
                id, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING));

        if (hasActiveSubscriptions) {
            throw new BadRequestException(
                    "No se puede eliminar el plan porque tiene suscripciones activas. Use desactivar en su lugar.");
        }

        planRepository.delete(plan);
    }

    public SubscriptionPlan getPlanOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de suscripción no encontrado"));
    }

    public SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationMonths(),
                plan.getActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
