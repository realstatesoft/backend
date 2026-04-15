package com.openroof.openroof.dto.preference;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para un rango numérico de preferencia (precio, superficie, habitaciones).
 */
public record RangeDTO(
        @NotBlank(message = "El nombre del campo es obligatorio")
        String fieldName,
        Double minValue,
        Double maxValue
) {}
