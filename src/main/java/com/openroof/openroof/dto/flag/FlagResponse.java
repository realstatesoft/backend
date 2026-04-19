package com.openroof.openroof.dto.flag;

import com.openroof.openroof.model.enums.FlagType;

import java.time.LocalDateTime;

public record FlagResponse(

        Long id,
        Long propertyId,
        FlagType flagType,
        String reason,
        String reportedByUsername,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolutionNotes
) {
}
