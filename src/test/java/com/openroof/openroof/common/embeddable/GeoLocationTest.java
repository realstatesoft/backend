package com.openroof.openroof.common.embeddable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoLocation — value object")
class GeoLocationTest {

    // ─── getLatAsDouble / getLngAsDouble ─────────────────────────────────────

    @Test
    @DisplayName("getLatAsDouble returns null when lat is null")
    void getLatAsDouble_null_returnsNull() {
        GeoLocation geo = new GeoLocation(null, new BigDecimal("55.0"));
        assertThat(geo.getLatAsDouble()).isNull();
    }

    @Test
    @DisplayName("getLatAsDouble returns double value when lat is set")
    void getLatAsDouble_value_returnsDouble() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.2867"), new BigDecimal("-57.6470"));
        assertThat(geo.getLatAsDouble()).isEqualTo(-25.2867, org.assertj.core.api.Assertions.within(0.00001));
    }

    @Test
    @DisplayName("getLngAsDouble returns null when lng is null")
    void getLngAsDouble_null_returnsNull() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.0"), null);
        assertThat(geo.getLngAsDouble()).isNull();
    }

    @Test
    @DisplayName("getLngAsDouble returns double value when lng is set")
    void getLngAsDouble_value_returnsDouble() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.2867"), new BigDecimal("-57.6470"));
        assertThat(geo.getLngAsDouble()).isEqualTo(-57.647, org.assertj.core.api.Assertions.within(0.0001));
    }

    // ─── hasCoordinates ──────────────────────────────────────────────────────

    @Test
    @DisplayName("hasCoordinates returns true when both lat and lng are present")
    void hasCoordinates_bothPresent_returnsTrue() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.0"), new BigDecimal("-57.0"));
        assertThat(geo.hasCoordinates()).isTrue();
    }

    @Test
    @DisplayName("hasCoordinates returns false when lat is null")
    void hasCoordinates_latNull_returnsFalse() {
        GeoLocation geo = new GeoLocation(null, new BigDecimal("-57.0"));
        assertThat(geo.hasCoordinates()).isFalse();
    }

    @Test
    @DisplayName("hasCoordinates returns false when lng is null")
    void hasCoordinates_lngNull_returnsFalse() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.0"), null);
        assertThat(geo.hasCoordinates()).isFalse();
    }

    @Test
    @DisplayName("hasCoordinates returns false when both are null")
    void hasCoordinates_bothNull_returnsFalse() {
        GeoLocation geo = new GeoLocation();
        assertThat(geo.hasCoordinates()).isFalse();
    }

    // ─── isValid ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValid returns true for coordinates within valid bounds")
    void isValid_validCoordinates_returnsTrue() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.2867"), new BigDecimal("-57.6470"));
        assertThat(geo.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid returns false when lat is null")
    void isValid_latNull_returnsFalse() {
        GeoLocation geo = new GeoLocation(null, new BigDecimal("-57.0"));
        assertThat(geo.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid returns false when lng is null")
    void isValid_lngNull_returnsFalse() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.0"), null);
        assertThat(geo.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid returns false when lat exceeds 90")
    void isValid_latAbove90_returnsFalse() {
        GeoLocation geo = new GeoLocation(new BigDecimal("91"), new BigDecimal("0"));
        assertThat(geo.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid returns false when lat is below -90")
    void isValid_latBelow_90_returnsFalse() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-91"), new BigDecimal("0"));
        assertThat(geo.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid returns false when lng exceeds 180")
    void isValid_lngAbove180_returnsFalse() {
        GeoLocation geo = new GeoLocation(new BigDecimal("0"), new BigDecimal("181"));
        assertThat(geo.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid returns false when lng is below -180")
    void isValid_lngBelow_180_returnsFalse() {
        GeoLocation geo = new GeoLocation(new BigDecimal("0"), new BigDecimal("-181"));
        assertThat(geo.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid accepts boundary values exactly at ±90 lat and ±180 lng")
    void isValid_boundaryValues_returnsTrue() {
        assertThat(new GeoLocation(new BigDecimal("90"), new BigDecimal("180")).isValid()).isTrue();
        assertThat(new GeoLocation(new BigDecimal("-90"), new BigDecimal("-180")).isValid()).isTrue();
    }

    // ─── toString ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString includes lat and lng values")
    void toString_includesLatLng() {
        GeoLocation geo = new GeoLocation(new BigDecimal("-25.29"), new BigDecimal("-57.65"));
        assertThat(geo.toString()).contains("-25.29").contains("-57.65");
    }
}
