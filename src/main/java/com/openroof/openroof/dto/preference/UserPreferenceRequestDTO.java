package com.openroof.openroof.dto.preference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO de entrada para crear o actualizar las preferencias de un usuario (upsert).
 */
public record UserPreferenceRequestDTO(
        @NotNull(message = "El ID de usuario es obligatorio")
        Long userId,

        @NotEmpty(message = "Debe seleccionar al menos una opción de preferencia")
        List<Long> selectedOptionIds,

        @Valid
        List<RangeDTO> ranges
) {}
