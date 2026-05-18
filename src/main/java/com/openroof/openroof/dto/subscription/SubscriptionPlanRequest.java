package com.openroof.openroof.dto.subscription;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SubscriptionPlanRequest(

        @NotBlank(message = "El nombre del plan es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El precio no puede tener más de 2 decimales")
        BigDecimal price,

        @NotNull(message = "La duración en meses es obligatoria")
        @Min(value = 1, message = "La duración mínima es 1 mes")
        @Max(value = 120, message = "La duración máxima es 120 meses")
        Integer durationMonths,

        Boolean active
) {}
