package com.openroof.openroof.dto.reservation;

import com.openroof.openroof.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        Long buyerId,
        String buyerName,
        String buyerEmail,
        BigDecimal amount,
        ReservationStatus status,
        LocalDateTime expiresAt,
        String notes,
        String cancelledReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}