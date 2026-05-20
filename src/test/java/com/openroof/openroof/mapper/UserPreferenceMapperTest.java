package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.preference.PreferenceCategoryDTO;
import com.openroof.openroof.dto.preference.PreferenceOptionDTO;
import com.openroof.openroof.dto.preference.RangeDTO;
import com.openroof.openroof.dto.preference.UserPreferenceResponseDTO;
import com.openroof.openroof.model.preference.PreferenceCategory;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code UserPreferenceMapper}.
 */
@DisplayName("UserPreferenceMapper")
class UserPreferenceMapperTest {

    private final UserPreferenceMapper mapper = new UserPreferenceMapper();

    // ─── toOptionDTO ────────────────────────────────────────────────────────

    /**
     * toOptionDTO_withCategory_mapsAllFields.
     */
    @Test
    @DisplayName("toOptionDTO_withCategory_mapsAllFields")
    void toOptionDTO_withCategory_mapsAllFields() {
        PreferenceCategory category = new PreferenceCategory();
        category.setCode("PROPERTY_TYPE");

        PreferenceOption option = new PreferenceOption();
        option.setId(1L);
        option.setLabel("Casa");
        option.setValue("HOUSE");
        option.setCategory(category);

        PreferenceOptionDTO dto = mapper.toOptionDTO(option);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.label()).isEqualTo("Casa");
        assertThat(dto.value()).isEqualTo("HOUSE");
        assertThat(dto.categoryCode()).isEqualTo("PROPERTY_TYPE");
    }

    /**
     * toOptionDTO_nullCategory_categoryCodeIsNull.
     */
    @Test
    @DisplayName("toOptionDTO_nullCategory_categoryCodeIsNull")
    void toOptionDTO_nullCategory_categoryCodeIsNull() {
        PreferenceOption option = new PreferenceOption();
        option.setId(2L);
        option.setLabel("Apto");
        option.setValue("APARTMENT");
        option.setCategory(null);

        PreferenceOptionDTO dto = mapper.toOptionDTO(option);

        assertThat(dto.categoryCode()).isNull();
        assertThat(dto.value()).isEqualTo("APARTMENT");
    }

    // ─── toCategoryDTO ──────────────────────────────────────────────────────

    /**
     * toCategoryDTO_nullOptions_returnsEmptyList.
     */
    @Test
    @DisplayName("toCategoryDTO_nullOptions_returnsEmptyList")
    void toCategoryDTO_nullOptions_returnsEmptyList() {
        PreferenceCategory category = new PreferenceCategory();
        category.setId(1L);
        category.setCode("ZONE");
        category.setName("Zona");
        category.setOptions(null);

        PreferenceCategoryDTO dto = mapper.toCategoryDTO(category);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.code()).isEqualTo("ZONE");
        assertThat(dto.name()).isEqualTo("Zona");
        assertThat(dto.options()).isEmpty();
    }

    /**
     * toCategoryDTO_optionsSortedByDisplayOrder.
     */
    @Test
    @DisplayName("toCategoryDTO_optionsSortedByDisplayOrder")
    void toCategoryDTO_optionsSortedByDisplayOrder() {
        PreferenceCategory category = new PreferenceCategory();
        category.setId(1L);
        category.setCode("PROP");
        category.setName("Tipo");

        PreferenceOption opt1 = new PreferenceOption();
        opt1.setId(10L); opt1.setLabel("C"); opt1.setValue("C"); opt1.setDisplayOrder(3);
        opt1.setCategory(category);

        PreferenceOption opt2 = new PreferenceOption();
        opt2.setId(11L); opt2.setLabel("A"); opt2.setValue("A"); opt2.setDisplayOrder(1);
        opt2.setCategory(category);

        PreferenceOption opt3 = new PreferenceOption();
        opt3.setId(12L); opt3.setLabel("B"); opt3.setValue("B"); opt3.setDisplayOrder(2);
        opt3.setCategory(category);

        category.setOptions(new ArrayList<>(List.of(opt1, opt2, opt3)));

        PreferenceCategoryDTO dto = mapper.toCategoryDTO(category);

        assertThat(dto.options()).extracting(PreferenceOptionDTO::value)
                .containsExactly("A", "B", "C");
    }

    /**
     * toCategoryDTO_nullDisplayOrder_treatedAsZero.
     */
    @Test
    @DisplayName("toCategoryDTO_nullDisplayOrder_treatedAsZero")
    void toCategoryDTO_nullDisplayOrder_treatedAsZero() {
        PreferenceCategory category = new PreferenceCategory();
        category.setId(2L);
        category.setCode("FEAT");
        category.setName("Feature");

        PreferenceOption optNull = new PreferenceOption();
        optNull.setId(20L); optNull.setLabel("NullOrder"); optNull.setValue("N");
        optNull.setDisplayOrder(null); optNull.setCategory(category);

        PreferenceOption opt2 = new PreferenceOption();
        opt2.setId(21L); opt2.setLabel("HasOrder"); opt2.setValue("H");
        opt2.setDisplayOrder(5); opt2.setCategory(category);

        category.setOptions(new ArrayList<>(List.of(opt2, optNull)));

        PreferenceCategoryDTO dto = mapper.toCategoryDTO(category);

        // null displayOrder treated as 0, so optNull sorts before opt2(5)
        assertThat(dto.options()).extracting(PreferenceOptionDTO::value)
                .containsExactly("N", "H");
    }

    // ─── toRangeDTO ─────────────────────────────────────────────────────────

    /**
     * toRangeDTO_mapsAllFields.
     */
    @Test
    @DisplayName("toRangeDTO_mapsAllFields")
    void toRangeDTO_mapsAllFields() {
        UserPreferenceRange range = new UserPreferenceRange();
        range.setFieldName("PRICE");
        range.setMinValue(100_000.0);
        range.setMaxValue(500_000.0);

        RangeDTO dto = mapper.toRangeDTO(range);

        assertThat(dto.fieldName()).isEqualTo("PRICE");
        assertThat(dto.minValue()).isEqualTo(100_000.0);
        assertThat(dto.maxValue()).isEqualTo(500_000.0);
    }

    /**
     * toRangeDTO_nullMinAndMax_keepsNulls.
     */
    @Test
    @DisplayName("toRangeDTO_nullMinAndMax_keepsNulls")
    void toRangeDTO_nullMinAndMax_keepsNulls() {
        UserPreferenceRange range = new UserPreferenceRange();
        range.setFieldName("BEDROOMS");
        range.setMinValue(null);
        range.setMaxValue(null);

        RangeDTO dto = mapper.toRangeDTO(range);

        assertThat(dto.minValue()).isNull();
        assertThat(dto.maxValue()).isNull();
    }

    // ─── toResponseDTO ──────────────────────────────────────────────────────

    /**
     * toResponseDTO_nullSelectedOptions_returnsEmptyOptionsList.
     */
    @Test
    @DisplayName("toResponseDTO_nullSelectedOptions_returnsEmptyOptionsList")
    void toResponseDTO_nullSelectedOptions_returnsEmptyOptionsList() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(99L);

        UserPreference pref = new UserPreference();
        pref.setUser(user);
        pref.setOnboardingCompleted(true);
        pref.setSelectedOptions(null);
        pref.setRanges(List.of());

        UserPreferenceResponseDTO dto = mapper.toResponseDTO(pref);

        assertThat(dto.userId()).isEqualTo(99L);
        assertThat(dto.onboardingCompleted()).isTrue();
        assertThat(dto.selectedOptions()).isEmpty();
        assertThat(dto.ranges()).isEmpty();
    }

    /**
     * toResponseDTO_nullRanges_returnsEmptyRangesList.
     */
    @Test
    @DisplayName("toResponseDTO_nullRanges_returnsEmptyRangesList")
    void toResponseDTO_nullRanges_returnsEmptyRangesList() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        UserPreference pref = new UserPreference();
        pref.setUser(user);
        pref.setOnboardingCompleted(false);
        pref.setSelectedOptions(Set.of());
        pref.setRanges(null);

        UserPreferenceResponseDTO dto = mapper.toResponseDTO(pref);

        assertThat(dto.ranges()).isEmpty();
        assertThat(dto.onboardingCompleted()).isFalse();
    }

    /**
     * toResponseDTO_withOptionsAndRanges_mapsAll.
     */
    @Test
    @DisplayName("toResponseDTO_withOptionsAndRanges_mapsAll")
    void toResponseDTO_withOptionsAndRanges_mapsAll() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);

        PreferenceCategory cat = new PreferenceCategory();
        cat.setCode("TYPE");

        PreferenceOption opt = new PreferenceOption();
        opt.setId(5L); opt.setLabel("Casa"); opt.setValue("HOUSE");
        opt.setDisplayOrder(1); opt.setCategory(cat);

        UserPreferenceRange range = new UserPreferenceRange();
        range.setFieldName("PRICE"); range.setMinValue(50_000.0); range.setMaxValue(200_000.0);

        UserPreference pref = new UserPreference();
        pref.setUser(user);
        pref.setOnboardingCompleted(true);
        pref.setSelectedOptions(Set.of(opt));
        pref.setRanges(List.of(range));

        UserPreferenceResponseDTO dto = mapper.toResponseDTO(pref);

        assertThat(dto.userId()).isEqualTo(7L);
        assertThat(dto.onboardingCompleted()).isTrue();
        assertThat(dto.selectedOptions()).hasSize(1);
        assertThat(dto.selectedOptions().get(0).value()).isEqualTo("HOUSE");
        assertThat(dto.ranges()).hasSize(1);
        assertThat(dto.ranges().get(0).fieldName()).isEqualTo("PRICE");
    }

    // ─── emptyResponseDTO ───────────────────────────────────────────────────

    /**
     * emptyResponseDTO_returnsEmptyPreferences.
     */
    @Test
    @DisplayName("emptyResponseDTO_returnsEmptyPreferences")
    void emptyResponseDTO_returnsEmptyPreferences() {
        UserPreferenceResponseDTO dto = mapper.emptyResponseDTO(42L);

        assertThat(dto.userId()).isEqualTo(42L);
        assertThat(dto.onboardingCompleted()).isFalse();
        assertThat(dto.selectedOptions()).isEmpty();
        assertThat(dto.ranges()).isEmpty();
    }
}
