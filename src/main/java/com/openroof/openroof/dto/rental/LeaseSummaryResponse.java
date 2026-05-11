package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.LeaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaseSummaryResponse(

        Long id,
        String propertyAddress,
        String tenantName,
        LeaseStatus status,
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate

) {}
