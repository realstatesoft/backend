package com.openroof.openroof.service;

import com.openroof.openroof.dto.location.LocationDto;
import com.openroof.openroof.mapper.LocationMapper;
import com.openroof.openroof.model.property.Location;
import com.openroof.openroof.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@code LocationService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService")
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    // ─── matchByCity ─────────────────────────────────────────────────────────

    /**
     * matchByCity with null input returns empty list without hitting repository.
     */
    @Test
    @DisplayName("matchByCity with null input returns empty list without hitting repository")
    void matchByCity_null_returnsEmpty() {
        List<LocationDto> result = locationService.matchByCity(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(locationRepository);
    }

    /**
     * matchByCity with blank string returns empty list without hitting repository.
     */
    @Test
    @DisplayName("matchByCity with blank string returns empty list without hitting repository")
    void matchByCity_blank_returnsEmpty() {
        List<LocationDto> result = locationService.matchByCity("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(locationRepository);
    }

    /**
     * matchByCity delegates to repository and maps each result.
     */
    @Test
    @DisplayName("matchByCity delegates to repository and maps each result")
    void matchByCity_validCity_returnsMappedDtos() {
        Location loc = Location.builder().name("Asunción, Central").city("Asunción").department("Central").country("Paraguay").build();
        loc.setId(1L);
        LocationDto dto = new LocationDto(1L, "Asunción, Central", "Asunción", "Central", "Paraguay", null, null, false);

        when(locationRepository.findByCityIgnoreCase("Asunción")).thenReturn(List.of(loc));
        when(locationMapper.toDto(loc)).thenReturn(dto);

        List<LocationDto> result = locationService.matchByCity("Asunción");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).city()).isEqualTo("Asunción");
        verify(locationRepository).findByCityIgnoreCase("Asunción");
    }

    /**
     * matchByCity trims whitespace before calling repository.
     */
    @Test
    @DisplayName("matchByCity trims whitespace before calling repository")
    void matchByCity_trimsInput() {
        when(locationRepository.findByCityIgnoreCase("Luque")).thenReturn(List.of());

        locationService.matchByCity("  Luque  ");

        verify(locationRepository).findByCityIgnoreCase("Luque");
    }

    /**
     * matchByCity with no repository matches returns empty list.
     */
    @Test
    @DisplayName("matchByCity with no repository matches returns empty list")
    void matchByCity_noMatches_returnsEmpty() {
        when(locationRepository.findByCityIgnoreCase("Nonexistent")).thenReturn(List.of());

        List<LocationDto> result = locationService.matchByCity("Nonexistent");

        assertThat(result).isEmpty();
    }

    // ─── findOrCreate ────────────────────────────────────────────────────────

    /**
     * findOrCreate with null city throws IllegalArgumentException.
     */
    @Test
    @DisplayName("findOrCreate with null city throws IllegalArgumentException")
    void findOrCreate_nullCity_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> locationService.findOrCreate(null, "Central", "Paraguay", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required");
    }

    /**
     * findOrCreate with blank city throws IllegalArgumentException.
     */
    @Test
    @DisplayName("findOrCreate with blank city throws IllegalArgumentException")
    void findOrCreate_blankCity_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> locationService.findOrCreate("  ", "Central", "Paraguay", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required");
    }

    /**
     * findOrCreate returns existing location when city already exists.
     */
    @Test
    @DisplayName("findOrCreate returns existing location when city already exists")
    void findOrCreate_existingCity_returnsExisting() {
        Location existing = Location.builder()
                .name("Luque, Central")
                .city("Luque")
                .department("Central")
                .country("Paraguay")
                .build();
        existing.setId(5L);
        LocationDto expectedDto = new LocationDto(5L, "Luque, Central", "Luque", "Central", "Paraguay", null, null, false);

        when(locationRepository.findByCityIgnoreCase("Luque")).thenReturn(List.of(existing));
        when(locationMapper.toDto(existing)).thenReturn(expectedDto);

        LocationDto result = locationService.findOrCreate("Luque", "Central", "Paraguay", null, null);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.city()).isEqualTo("Luque");
        verify(locationRepository, never()).save(any());
    }

    /**
     * findOrCreate creates and saves new location when city does not exist.
     */
    @Test
    @DisplayName("findOrCreate creates and saves new location when city does not exist")
    void findOrCreate_newCity_savesAndReturnsWithIsNewTrue() {
        Location saved = Location.builder()
                .name("Capiatá, Central")
                .city("Capiatá")
                .department("Central")
                .country("Paraguay")
                .build();
        saved.setId(99L);
        LocationDto mappedDto = new LocationDto(99L, "Capiatá, Central", "Capiatá", "Central", "Paraguay", null, null, false);

        when(locationRepository.findByCityIgnoreCase("Capiatá")).thenReturn(List.of());
        when(locationRepository.save(any(Location.class))).thenReturn(saved);
        when(locationMapper.toDto(saved)).thenReturn(mappedDto);

        LocationDto result = locationService.findOrCreate("Capiatá", "Central", "Paraguay", null, null);

        assertThat(result.isNew()).isTrue();
        assertThat(result.city()).isEqualTo("Capiatá");
        verify(locationRepository).save(any(Location.class));
    }
}
