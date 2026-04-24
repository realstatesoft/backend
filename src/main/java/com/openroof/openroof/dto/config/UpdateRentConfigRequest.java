package com.openroof.openroof.dto.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateRentConfigRequest(
        @NotNull
        @Min(1)
        @Max(12)
        Integer depositMonths,

        @NotNull
        @Digits(integer = 3, fraction = 2)
        @DecimalMin(value = "0.00", inclusive = false)
        @DecimalMax("100.00")
        BigDecimal commissionPercent
) {}