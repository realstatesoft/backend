package com.openroof.openroof.service;

import com.openroof.openroof.model.admin.PropertyFlag;
import com.openroof.openroof.model.enums.FlagType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyFlagRepository;
import com.openroof.openroof.repository.PropertyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyFlagServiceTest {

    @Mock
    private PropertyFlagRepository propertyFlagRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyFlagService propertyFlagService;

    @Test
    @DisplayName("getFlagsByStatus() devuelve solo flags activos por defecto")
    void getFlagsByStatus_defaultsToActive() {
        PropertyFlag active = buildFlag(1L, null);
        when(propertyFlagRepository.findAllByResolvedAtIsNull()).thenReturn(List.of(active));

        var result = propertyFlagService.getFlagsByStatus(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().flagType()).isEqualTo(FlagType.FRAUD);
    }

    @Test
    @DisplayName("getFlagsByStatus() devuelve flags resueltos cuando status=RESOLVED")
    void getFlagsByStatus_resolvedReturnsResolvedFlags() {
        PropertyFlag resolved = buildFlag(2L, java.time.LocalDateTime.now());
        when(propertyFlagRepository.findAllByResolvedAtIsNotNull()).thenReturn(List.of(resolved));

        var result = propertyFlagService.getFlagsByStatus("RESOLVED");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getFlagsByStatus() devuelve ambos conjuntos cuando status=ALL")
    void getFlagsByStatus_allReturnsCombinedFlags() {
        PropertyFlag active = buildFlag(1L, null);
        PropertyFlag resolved = buildFlag(2L, java.time.LocalDateTime.now());

        when(propertyFlagRepository.findAllByResolvedAtIsNull()).thenReturn(List.of(active));
        when(propertyFlagRepository.findAllByResolvedAtIsNotNull()).thenReturn(List.of(resolved));

        var result = propertyFlagService.getFlagsByStatus("ALL");

        assertThat(result).extracting("id").containsExactly(1L, 2L);
    }

    private PropertyFlag buildFlag(Long id, java.time.LocalDateTime resolvedAt) {
        Property property = Property.builder().build();
        property.setId(100L);

        User reporter = User.builder().email("reporter@test.com").build();
        reporter.setId(10L);

        PropertyFlag flag = PropertyFlag.builder()
                .property(property)
                .reportedBy(reporter)
                .flagType(FlagType.FRAUD)
                .reason("reason")
                .resolvedAt(resolvedAt)
                .build();
        flag.setId(id);
        return flag;
    }
}
