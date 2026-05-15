package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.RentalApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalApplicationResponse(

        Long id,
        Long propertyId,
        String propertyAddress,
        Long applicantId,
        String applicantName,
        RentalApplicationStatus status,
        String message,
        BigDecimal monthlyIncome,
        EmploymentStatus employmentStatus,
        Integer numberOfOccupants,
        Boolean hasPets,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt

) {}
