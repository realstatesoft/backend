package com.openroof.openroof.service;

import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.preference.PreferenceCategory;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import com.openroof.openroof.model.property.ExteriorFeature;
import com.openroof.openroof.model.property.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code PropertyRelevanceService}.
 */
@DisplayName("PropertyRelevanceService")
class PropertyRelevanceServiceTest {

    private final PropertyRelevanceService service = new PropertyRelevanceService();

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PreferenceOption optionFor(String categoryCode, String value, String label) {
        PreferenceCategory cat = PreferenceCategory.builder().code(categoryCode).build();
        return PreferenceOption.builder().category(cat).value(value).label(label).build();
    }

    private UserPreferenceRange rangeFor(String field, Double min, Double max) {
        return UserPreferenceRange.builder().fieldName(field).minValue(min).maxValue(max).build();
    }

    /** A minimal property with type, address and price. */
    private Property baseProperty() {
        return Property.builder()
                .propertyType(PropertyType.HOUSE)
                .address("Av. Corrientes 500 Buenos Aires")
                .price(BigDecimal.valueOf(200_000))
                .bedrooms(3)
                .build();
    }

    // ─── Score = 0 (no preferences) ──────────────────────────────────────────

    /**
     * calculateScore_noSelectedOptions_returns0.
     */
    @Test
    @DisplayName("calculateScore_noSelectedOptions_returns0")
    void calculateScore_noSelectedOptions_returns0() {
        UserPreference pref = UserPreference.builder().build(); // defaults: empty set + empty list

        int score = service.calculateScore(baseProperty(), pref);

        assertThat(score).isZero();
    }

    // ─── Criterion 1 – Property type (30 pts) ────────────────────────────────

    /**
     * calculateScore_propertyTypeMatch_adds30.
     */
    @Test
    @DisplayName("calculateScore_propertyTypeMatch_adds30")
    void calculateScore_propertyTypeMatch_adds30() {
        PreferenceOption opt = optionFor("PROPERTY_TYPE", "HOUSE", "Casa");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(opt)).build();

        int score = service.calculateScore(baseProperty(), pref);

        assertThat(score).isEqualTo(30);
    }

    /**
     * calculateScore_propertyTypeNoMatch_adds0.
     */
    @Test
    @DisplayName("calculateScore_propertyTypeNoMatch_adds0")
    void calculateScore_propertyTypeNoMatch_adds0() {
        PreferenceOption opt = optionFor("PROPERTY_TYPE", "APARTMENT", "Departamento");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(opt)).build();

        int score = service.calculateScore(baseProperty(), pref);

        assertThat(score).isZero();
    }

    // ─── Criterion 2 – Zone / city (25 pts) ──────────────────────────────────

    /**
     * calculateScore_zoneMatchInAddress_adds25.
     */
    @Test
    @DisplayName("calculateScore_zoneMatchInAddress_adds25")
    void calculateScore_zoneMatchInAddress_adds25() {
        // label "buenos aires" found in address "Av. Corrientes 500 Buenos Aires"
        PreferenceOption opt = optionFor("ZONE", "BUE", "Buenos Aires");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(opt)).build();

        int score = service.calculateScore(baseProperty(), pref);

        assertThat(score).isEqualTo(25);
    }

    /**
     * calculateScore_zoneNoMatchInAddress_adds0.
     */
    @Test
    @DisplayName("calculateScore_zoneNoMatchInAddress_adds0")
    void calculateScore_zoneNoMatchInAddress_adds0() {
        // label "rosario" not in address "... Buenos Aires"
        PreferenceOption opt = optionFor("ZONE", "ROS", "Rosario");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(opt)).build();

        int score = service.calculateScore(baseProperty(), pref);

        assertThat(score).isZero();
    }

    /**
     * calculateScore_zonePreference_nullAddress_adds0.
     */
    @Test
    @DisplayName("calculateScore_zonePreference_nullAddress_adds0")
    void calculateScore_zonePreference_nullAddress_adds0() {
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE)
                .address(null) // no address
                .price(BigDecimal.valueOf(200_000))
                .build();

        PreferenceOption opt = optionFor("ZONE", "BUE", "Buenos Aires");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(opt)).build();

        int score = service.calculateScore(prop, pref);

        assertThat(score).isZero();
    }

    // ─── Criterion 3 – Price range (20 pts) ──────────────────────────────────

    /**
     * calculateScore_priceWithinRange_adds20.
     */
    @Test
    @DisplayName("calculateScore_priceWithinRange_adds20")
    void calculateScore_priceWithinRange_adds20() {
        UserPreferenceRange range = rangeFor("PRICE", 100_000.0, 500_000.0);
        UserPreference pref = UserPreference.builder()
                .ranges(List.of(range)).build();

        int score = service.calculateScore(baseProperty(), pref); // price 200_000

        assertThat(score).isEqualTo(20);
    }

    /**
     * calculateScore_priceBelowMin_adds0.
     */
    @Test
    @DisplayName("calculateScore_priceBelowMin_adds0")
    void calculateScore_priceBelowMin_adds0() {
        UserPreferenceRange range = rangeFor("PRICE", 300_000.0, 500_000.0);
        UserPreference pref = UserPreference.builder()
                .ranges(List.of(range)).build();

        int score = service.calculateScore(baseProperty(), pref); // price 200_000 < min 300_000

        assertThat(score).isZero();
    }

    /**
     * calculateScore_priceAboveMax_adds0.
     */
    @Test
    @DisplayName("calculateScore_priceAboveMax_adds0")
    void calculateScore_priceAboveMax_adds0() {
        UserPreferenceRange range = rangeFor("PRICE", 10_000.0, 100_000.0);
        UserPreference pref = UserPreference.builder()
                .ranges(List.of(range)).build();

        int score = service.calculateScore(baseProperty(), pref); // price 200_000 > max 100_000

        assertThat(score).isZero();
    }

    // ─── Criterion 4 – Bedrooms range (15 pts) ───────────────────────────────

    /**
     * calculateScore_bedroomsInRange_adds15.
     */
    @Test
    @DisplayName("calculateScore_bedroomsInRange_adds15")
    void calculateScore_bedroomsInRange_adds15() {
        UserPreferenceRange range = rangeFor("BEDROOMS", 2.0, 5.0);
        UserPreference pref = UserPreference.builder()
                .ranges(List.of(range)).build();

        int score = service.calculateScore(baseProperty(), pref); // bedrooms = 3

        assertThat(score).isEqualTo(15);
    }

    /**
     * calculateScore_bedroomsLessThanMin_adds0.
     */
    @Test
    @DisplayName("calculateScore_bedroomsLessThanMin_adds0")
    void calculateScore_bedroomsLessThanMin_adds0() {
        UserPreferenceRange range = rangeFor("BEDROOMS", 5.0, null);
        UserPreference pref = UserPreference.builder()
                .ranges(List.of(range)).build();

        int score = service.calculateScore(baseProperty(), pref); // bedrooms = 3 < min 5

        assertThat(score).isZero();
    }

    // ─── Criterion 5 – Exterior features (5 pts each, capped at 10) ──────────

    /**
     * calculateScore_oneExteriorFeatureMatch_adds5.
     */
    @Test
    @DisplayName("calculateScore_oneExteriorFeatureMatch_adds5")
    void calculateScore_oneExteriorFeatureMatch_adds5() {
        ExteriorFeature pool = new ExteriorFeature();
        pool.setName("Pool");
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE).address("Addr").price(BigDecimal.valueOf(200_000))
                .exteriorFeatures(List.of(pool)).build();

        PreferenceOption opt = optionFor("EXTERIOR_FEATURE", "POOL", "Pool");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(opt)).build();

        int score = service.calculateScore(prop, pref);

        assertThat(score).isEqualTo(5);
    }

    /**
     * calculateScore_twoExteriorFeatureMatches_adds10.
     */
    @Test
    @DisplayName("calculateScore_twoExteriorFeatureMatches_adds10")
    void calculateScore_twoExteriorFeatureMatches_adds10() {
        ExteriorFeature pool = new ExteriorFeature();
        pool.setName("Pool");
        ExteriorFeature garden = new ExteriorFeature();
        garden.setName("Garden");
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE).address("Addr").price(BigDecimal.valueOf(200_000))
                .exteriorFeatures(List.of(pool, garden)).build();

        PreferenceOption optPool = optionFor("EXTERIOR_FEATURE", "POOL", "Pool");
        PreferenceOption optGarden = optionFor("EXTERIOR_FEATURE", "GARDEN", "Garden");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(optPool, optGarden)).build();

        int score = service.calculateScore(prop, pref);

        assertThat(score).isEqualTo(10);
    }

    /**
     * calculateScore_moreExteriorFeatureMatches_cappedAt10.
     */
    @Test
    @DisplayName("calculateScore_moreExteriorFeatureMatches_cappedAt10")
    void calculateScore_moreExteriorFeatureMatches_cappedAt10() {
        ExteriorFeature pool = new ExteriorFeature();
        pool.setName("Pool");
        ExteriorFeature garden = new ExteriorFeature();
        garden.setName("Garden");
        ExteriorFeature terrace = new ExteriorFeature();
        terrace.setName("Terrace");
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE).address("Addr").price(BigDecimal.valueOf(200_000))
                .exteriorFeatures(List.of(pool, garden, terrace)).build();

        PreferenceOption optPool = optionFor("EXTERIOR_FEATURE", "POOL", "Pool");
        PreferenceOption optGarden = optionFor("EXTERIOR_FEATURE", "GARDEN", "Garden");
        PreferenceOption optTerrace = optionFor("EXTERIOR_FEATURE", "TERRACE", "Terrace");
        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(optPool, optGarden, optTerrace)).build();

        int score = service.calculateScore(prop, pref);

        // 3 matches × 5 = 15, but capped at 10
        assertThat(score).isEqualTo(10);
    }

    // ─── Perfect match ────────────────────────────────────────────────────────

    /**
     * calculateScore_perfectMatch_returns100.
     */
    @Test
    @DisplayName("calculateScore_perfectMatch_returns100")
    void calculateScore_perfectMatch_returns100() {
        // type match (30) + zone (25) + price in range (20) + bedrooms in range (15) + 2 features (10) = 100
        ExteriorFeature pool = new ExteriorFeature();
        pool.setName("Pool");
        ExteriorFeature garden = new ExteriorFeature();
        garden.setName("Garden");

        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE)
                .address("Av. Corrientes 500 Buenos Aires")
                .price(BigDecimal.valueOf(200_000))
                .bedrooms(3)
                .exteriorFeatures(List.of(pool, garden))
                .build();

        PreferenceOption typeOpt = optionFor("PROPERTY_TYPE", "HOUSE", "Casa");
        PreferenceOption zoneOpt = optionFor("ZONE", "BUE", "Buenos Aires");
        PreferenceOption poolOpt = optionFor("EXTERIOR_FEATURE", "POOL", "Pool");
        PreferenceOption gardenOpt = optionFor("EXTERIOR_FEATURE", "GARDEN", "Garden");

        UserPreferenceRange priceRange = rangeFor("PRICE", 100_000.0, 500_000.0);
        UserPreferenceRange bedroomRange = rangeFor("BEDROOMS", 2.0, 5.0);

        UserPreference pref = UserPreference.builder()
                .selectedOptions(Set.of(typeOpt, zoneOpt, poolOpt, gardenOpt))
                .ranges(List.of(priceRange, bedroomRange))
                .build();

        int score = service.calculateScore(prop, pref);

        assertThat(score).isEqualTo(100);
    }
}
