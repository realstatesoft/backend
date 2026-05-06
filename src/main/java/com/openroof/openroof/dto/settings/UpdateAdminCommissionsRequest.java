package com.openroof.openroof.dto.settings;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateAdminCommissionsRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal saleCommissionPercent,

        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal rentCommissionPercent,

        @NotNull @Min(1) @Max(12)
        Integer rentDepositMonths
) {}
