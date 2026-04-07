package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractSummaryResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        String buyerName,
        String sellerName,
        ContractType contractType,
        ContractStatus status,
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {}
