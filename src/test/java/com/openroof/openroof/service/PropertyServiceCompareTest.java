package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.ExteriorFeatureRepository;
import com.openroof.openroof.repository.InteriorFeatureRepository;
import com.openroof.openroof.repository.LocationRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.PropertyViewRepository;
import com.openroof.openroof.repository.UserPreferenceRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceCompareTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private PropertyViewRepository propertyViewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AgentProfileRepository agentProfileRepository;
    @Mock
    private ExteriorFeatureRepository exteriorFeatureRepository;
    @Mock
    private InteriorFeatureRepository interiorFeatureRepository;
    @Mock
    private PropertyMapper propertyMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private UserPreferenceRepository userPreferenceRepository;
    @Mock
    private PropertyRelevanceService propertyRelevanceService;

    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(
                propertyRepository,
                propertyViewRepository,
                userRepository,
                locationRepository,
                agentProfileRepository,
                exteriorFeatureRepository,
                interiorFeatureRepository,
                propertyMapper,
                notificationService,
                auditService,
                userPreferenceRepository,
                propertyRelevanceService
        );
    }

    @Test
    void getForComparison_preservesInputOrder() {
        Property first = Property.builder().title("A").build();
        first.setId(1L);
        Property second = Property.builder().title("B").build();
        second.setId(2L);
        Property third = Property.builder().title("C").build();
        third.setId(3L);

        when(propertyRepository.findAllById(any())).thenReturn(List.of(first, third, second));
        when(propertyMapper.toSummaryResponse(first)).thenReturn(summary(1L, "A"));
        when(propertyMapper.toSummaryResponse(second)).thenReturn(summary(2L, "B"));
        when(propertyMapper.toSummaryResponse(third)).thenReturn(summary(3L, "C"));

        List<PropertySummaryResponse> result = propertyService.getForComparison(List.of(3L, 1L, 2L));

        assertEquals(List.of(3L, 1L, 2L), result.stream().map(PropertySummaryResponse::id).toList());
    }

    @Test
    void getForComparison_rejectsMoreThanThreeProperties() {
        assertThrows(BadRequestException.class,
                () -> propertyService.getForComparison(List.of(1L, 2L, 3L, 4L)));
    }

    @Test
    void getForComparison_failsWhenAnyPropertyIsMissing() {
        Property first = Property.builder().title("A").build();
        first.setId(1L);

        when(propertyRepository.findAllById(any())).thenReturn(List.of(first));

        assertThrows(ResourceNotFoundException.class,
                () -> propertyService.getForComparison(List.of(1L, 99L)));
    }

    private PropertySummaryResponse summary(Long id, String title) {
        return new PropertySummaryResponse(
                id,
                title,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0
        );
    }
}
