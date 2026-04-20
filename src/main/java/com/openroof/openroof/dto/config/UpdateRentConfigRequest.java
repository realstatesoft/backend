package com.openroof.openroof.dto.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateRentConfigRequest(
        @NotNull @Min(0) @Max(24) Integer depositMonths,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionPercent
) {}