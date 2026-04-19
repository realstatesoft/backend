package com.openroof.openroof.dto.preference;

import java.util.List;

/**
 * DTO de una categoría de preferencias con sus opciones anidadas.
 */
public record PreferenceCategoryDTO(
        Long id,
        String code,
        String name,
        List<PreferenceOptionDTO> options
) {}
