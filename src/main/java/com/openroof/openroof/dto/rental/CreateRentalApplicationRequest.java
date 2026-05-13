package com.openroof.openroof.dto.rental;

import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.validation.EmployerNameRequiredIfEmployed;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@EmployerNameRequiredIfEmployed
public record CreateRentalApplicationRequest(

        @NotNull Long propertyId,
        @NotBlank String message,
        @NotNull @DecimalMin(value = "0.01", message = "monthlyIncome debe ser mayor a 0") BigDecimal monthlyIncome,
        @NotNull EmploymentStatus employmentStatus,
        String employerName,
        @NotNull @Size(min = 2, message = "Debés indicar al menos 2 referencias") List<String> references,
        Integer numberOfOccupants,
        Boolean hasPets,
        @NotNull @AssertTrue(message = "Debés aceptar el consentimiento de screening") Boolean screeningConsent

) {}
