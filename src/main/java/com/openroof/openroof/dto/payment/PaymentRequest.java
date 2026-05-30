package com.openroof.openroof.dto.payment;

import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.payment.PaymentMetadata;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "El tipo de pago es obligatorio")
        PaymentType type,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto no puede tener más de 2 decimales")
        BigDecimal amount,

        @NotBlank(message = "El concepto no puede estar vacío")
        @Size(max = 255, message = "El concepto no puede exceder 255 caracteres")
        String concept,

        PaymentMetadata metadata
) {}
