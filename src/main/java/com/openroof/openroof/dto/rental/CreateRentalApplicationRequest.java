package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.EmploymentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRentalApplicationRequest(

        @NotNull Long propertyId,
        @NotBlank String message,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyIncome,
        @NotNull EmploymentStatus employmentStatus,
        @NotBlank String employerName,
        Integer numberOfOccupants,
        Boolean hasPets,
        @AssertTrue Boolean acceptsTerms

) {}
