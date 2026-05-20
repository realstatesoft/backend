package com.openroof.openroof.dto.subscription;

import com.openroof.openroof.model.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        Long userId,
        String userName,
        SubscriptionPlanResponse plan,
        SubscriptionStatus status,
        Long paymentId,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
