package com.openroof.openroof.dto.preference;

import java.util.List;

/**
 * DTO de respuesta con las preferencias guardadas de un usuario.
 * Si el usuario no completó el onboarding, onboardingCompleted = false y las listas están vacías.
 */
public record UserPreferenceResponseDTO(
        Long userId,
        Boolean onboardingCompleted,
        List<PreferenceOptionDTO> selectedOptions,
        List<RangeDTO> ranges
) {}
