package com.openroof.openroof.dto.flag;

import com.openroof.openroof.model.enums.FlagType;

import java.time.LocalDateTime;

public record FlagSummaryResponse(
        Long id,
        Long propertyId,
        FlagType flagType,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
