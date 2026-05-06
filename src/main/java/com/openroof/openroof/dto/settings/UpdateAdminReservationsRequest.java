package com.openroof.openroof.dto.settings;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateAdminReservationsRequest(
        @NotNull @Min(1) @Max(720)
        Integer ttlHours,

        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal depositPercent
) {}
