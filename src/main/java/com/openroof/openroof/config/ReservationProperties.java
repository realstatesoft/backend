package com.openroof.openroof.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "openroof.reservation")
public record ReservationProperties(
        @NotNull @Min(1) Integer ttlHours,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal defaultPercent
) {}
