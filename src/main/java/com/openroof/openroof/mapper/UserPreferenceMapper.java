package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.model.preference.PreferenceCategory;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para el módulo de preferencias de usuario.
 * Sigue el mismo patrón que {@link PropertyMapper}.
 */
@Component
public class UserPreferenceMapper {

    public PreferenceOptionDTO toOptionDTO(PreferenceOption option) {
        return new PreferenceOptionDTO(
                option.getId(),
                option.getLabel(),
                option.getValue(),
                option.getCategory() != null ? option.getCategory().getCode() : null
        );
    }

    public PreferenceCategoryDTO toCategoryDTO(PreferenceCategory category) {
        List<PreferenceOptionDTO> optionDTOs = category.getOptions() == null
                ? Collections.emptyList()
                : category.getOptions().stream()
                        .sorted((a, b) -> {
                            int oa = a.getDisplayOrder() != null ? a.getDisplayOrder() : 0;
                            int ob = b.getDisplayOrder() != null ? b.getDisplayOrder() : 0;
                            return Integer.compare(oa, ob);
                        })
                        .map(this::toOptionDTO)
                        .toList();

        return new PreferenceCategoryDTO(
                category.getId(),
                category.getCode(),
                category.getName(),
                optionDTOs
        );
    }

    public RangeDTO toRangeDTO(UserPreferenceRange range) {
        return new RangeDTO(
                range.getFieldName(),
                range.getMinValue(),
                range.getMaxValue()
        );
    }

    public UserPreferenceResponseDTO toResponseDTO(UserPreference pref) {
        List<PreferenceOptionDTO> options = pref.getSelectedOptions() == null
                ? Collections.emptyList()
                : pref.getSelectedOptions().stream()
                        .map(this::toOptionDTO)
                        .toList();

        List<RangeDTO> ranges = pref.getRanges() == null
                ? Collections.emptyList()
                : pref.getRanges().stream()
                        .map(this::toRangeDTO)
                        .toList();

        return new UserPreferenceResponseDTO(
                pref.getUser().getId(),
                pref.isOnboardingCompleted(),
                options,
                ranges
        );
    }

    /**
     * Construye un DTO de respuesta vacío para usuarios sin preferencias guardadas.
     */
    public UserPreferenceResponseDTO emptyResponseDTO(Long userId) {
        return new UserPreferenceResponseDTO(userId, false, Collections.emptyList(), Collections.emptyList());
    }
}
