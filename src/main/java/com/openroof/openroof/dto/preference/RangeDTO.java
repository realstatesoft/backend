package com.openroof.openroof.dto.preference;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO para un rango numérico de preferencia (precio, superficie, habitaciones).
 */
public record RangeDTO(
        @NotBlank(message = "El nombre del campo es obligatorio")
        String fieldName,

        @PositiveOrZero(message = "El valor mínimo no puede ser negativo")
        Double minValue,

        @PositiveOrZero(message = "El valor máximo no puede ser negativo")
        Double maxValue
) {
    @AssertTrue(message = "El valor mínimo no puede ser mayor que el valor máximo")
    public boolean isValidRange() {
        if (minValue == null || maxValue == null) {
            return true;
        }
        return minValue <= maxValue;
    }
}
