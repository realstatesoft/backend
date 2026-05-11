package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.RentalApplicationStatus;

import java.time.LocalDateTime;

public record RentalApplicationSummaryResponse(

        Long id,
        String propertyAddress,
        String applicantName,
        RentalApplicationStatus status,
        LocalDateTime submittedAt

) {}
