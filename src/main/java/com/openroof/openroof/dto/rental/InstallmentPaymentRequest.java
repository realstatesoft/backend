package com.openroof.openroof.dto.rental;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallmentPaymentRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto no puede tener más de 2 decimales")
        BigDecimal amount,

        String method,

        String notes
) {}
