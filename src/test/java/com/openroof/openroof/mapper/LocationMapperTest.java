package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.location.LocationDto;
import com.openroof.openroof.model.property.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code LocationMapper — toDto}.
 */
@DisplayName("LocationMapper — toDto")
class LocationMapperTest {

    private final LocationMapper mapper = new LocationMapper();

    /**
     * null input returns null.
     */
    @Test
    @DisplayName("null input returns null")
    void toDto_null_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    /**
     * fully populated Location maps all fields correctly.
     */
    @Test
    @DisplayName("fully populated Location maps all fields correctly")
    void toDto_fullLocation_mapsAllFields() {
        Location location = Location.builder()
                .name("Asunción, Central")
                .city("Asunción")
                .department("Central")
                .country("Paraguay")
                .build();
        location.setId(7L);

        LocationDto dto = mapper.toDto(location);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Asunción, Central");
        assertThat(dto.city()).isEqualTo("Asunción");
        assertThat(dto.department()).isEqualTo("Central");
        assertThat(dto.country()).isEqualTo("Paraguay");
        assertThat(dto.isNew()).isFalse();
    }

    /**
     * Location without coordinates maps lat/lng as null.
     */
    @Test
    @DisplayName("Location without coordinates maps lat/lng as null")
    void toDto_noCoordinates_latLngNull() {
        Location location = Location.builder()
                .name("Ciudad del Este")
                .city("Ciudad del Este")
                .department("Alto Paraná")
                .country("Paraguay")
                .build();

        LocationDto dto = mapper.toDto(location);

        assertThat(dto.lat()).isNull();
        assertThat(dto.lng()).isNull();
    }

    /**
     * isNew is always false — mapper never marks as new.
     */
    @Test
    @DisplayName("isNew is always false — mapper never marks as new")
    void toDto_isNewAlwaysFalse() {
        Location location = Location.builder()
                .name("Luque")
                .city("Luque")
                .department("Central")
                .country("Paraguay")
                .build();

        assertThat(mapper.toDto(location).isNew()).isFalse();
    }
}
