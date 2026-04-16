package com.openroof.openroof.dto.preference;

/**
 * DTO de una opción individual de preferencia.
 */
public record PreferenceOptionDTO(
        Long id,
        String label,
        String value,
        String categoryCode
) {}
