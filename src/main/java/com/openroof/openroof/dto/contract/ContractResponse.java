package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        Long buyerId,
        String buyerName,
        String buyerEmail,
        Long sellerId,
        String sellerName,
        String sellerEmail,
        Long listingAgentId,
        String listingAgentName,
        Long buyerAgentId,
        String buyerAgentName,
        Long templateId,
        ContractType contractType,
        ContractStatus status,
        BigDecimal amount,
        BigDecimal commissionPct,
        BigDecimal listingAgentCommissionPct,
        BigDecimal buyerAgentCommissionPct,
        LocalDate startDate,
        LocalDate endDate,
        String terms,
        String documentUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
