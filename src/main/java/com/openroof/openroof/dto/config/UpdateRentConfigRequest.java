package com.openroof.openroof.dto.config;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateRentConfigRequest(
        @NotNull Integer depositMonths,
        @NotNull @Digits(integer = 3, fraction = 2) BigDecimal commissionPercent
) {}