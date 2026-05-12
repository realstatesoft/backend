package com.openroof.openroof.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TenantInstallmentItem(
    Long id,
    int installmentNumber,
    String period,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal balance,
    String status,
    LocalDate dueDate,
    List<LeasePaymentInfo> payments
) {
    public record LeasePaymentInfo(
        String method,
        BigDecimal amount,
        LocalDateTime date,
        String receiptUrl
    ) {}
}
