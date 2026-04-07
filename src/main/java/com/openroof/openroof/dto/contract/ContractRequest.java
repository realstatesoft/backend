package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(
        @NotNull Long propertyId,
        @NotNull Long buyerId,
        @NotNull Long sellerId,
        Long listingAgentId,
        Long buyerAgentId,
        Long templateId,
        @NotNull ContractType contractType,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        BigDecimal commissionPct,
        BigDecimal listingAgentCommissionPct,
        BigDecimal buyerAgentCommissionPct,
        LocalDate startDate,
        LocalDate endDate,
        String terms
) {}
