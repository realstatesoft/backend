package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RentalInstallmentResponse(

        Long id,
        Long leaseId,
        Integer installmentNumber,
        BigDecimal amount,
        BigDecimal lateFeeAmount,
        BigDecimal totalAmount,
        LocalDate dueDate,
        LocalDate paidDate,
        InstallmentStatus status,
        String notes,
        LocalDateTime createdAt,
        String currency,
        List<LeasePaymentResponse> payments

) {}
