package com.openroof.openroof.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionPlanResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer durationMonths,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
