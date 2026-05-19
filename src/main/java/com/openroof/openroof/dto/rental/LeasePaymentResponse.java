package com.openroof.openroof.dto.rental;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeasePaymentResponse(
        Long id,
        Long leaseId,
        Long installmentId,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String type,
        LocalDateTime paidAt,
        String receiptPdfUrl
) {
}