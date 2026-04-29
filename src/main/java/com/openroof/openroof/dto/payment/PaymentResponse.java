package com.openroof.openroof.dto.payment;

import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long userId,
        String userName,
        PaymentType type,
        PaymentStatus status,
        String concept,
        String transactionCode,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
