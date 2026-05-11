package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaseResponse(

        Long id,
        Long propertyId,
        String propertyAddress,
        Long landlordId,
        Long tenantId,
        String tenantName,
        LeaseType leaseType,
        LeaseStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        DepositStatus depositStatus,
        BillingFrequency billingFrequency,
        LocalDateTime signedAt,
        LocalDateTime activatedAt,
        LocalDateTime createdAt

) {}
