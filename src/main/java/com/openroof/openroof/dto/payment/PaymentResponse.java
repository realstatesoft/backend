package com.openroof.openroof.dto.payment;

import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.payment.PaymentMetadata;

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
        PaymentMetadata metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
