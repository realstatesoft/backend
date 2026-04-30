package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.property.Location;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.repository.*;
import com.openroof.openroof.repository.UserPreferenceRepository;
import com.openroof.openroof.mapper.PropertyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertySimilarTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PropertyViewRepository propertyViewRepository;
    @Mock private UserRepository userRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private ExteriorFeatureRepository exteriorFeatureRepository;
    @Mock private InteriorFeatureRepository interiorFeatureRepository;
    @Mock private HighlightRepository highlightRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PropertyMapper propertyMapper;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private PropertyRelevanceService propertyRelevanceService;

    private PropertyService propertyService;

    private Property baseProperty;
    private Property nearbyProperty1;
    private Property nearbyProperty2;
    private PropertySummaryResponse summaryResponse1;
    private PropertySummaryResponse summaryResponse2;

    private static final Long PROPERTY_ID = 1L;
    private static final BigDecimal BASE_PRICE = new BigDecimal("150000");

    private PropertySummaryResponse createSummaryResponse(String title) {
        return new PropertySummaryResponse(
                null,               // id
                title,              // title
                BASE_PRICE,         // price
                "APARTMENT",        // propertyType
                "RESIDENTIAL",      // category
                "Calle Falsa 123",  // address
                null,               // primaryImageUrl
                2,                  // bedrooms
                new BigDecimal("2"),// bathrooms
                new BigDecimal("80"),// surfaceArea
                "PUBLISHED",        // status
                "Asuncion",         // locationName
                new BigDecimal("-25.2637"), // lat
                new BigDecimal("-57.5759"), // lng
                null,               // trashedAt
                0,                   // relevanceScore
                false,              // hihghlight
                null                // highlighted until
        );
    }

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
                highlightRepository,
                paymentRepository,
                propertyMapper,
                notificationService,
                auditService,
                userPreferenceRepository,
                propertyRelevanceService
        );

        Location location = Location.builder()
                .city("Asuncion")
                .country("Paraguay")
                .build();

        baseProperty = mock(Property.class);
        when(baseProperty.getId()).thenReturn(PROPERTY_ID);
        when(baseProperty.getPrice()).thenReturn(BASE_PRICE);
        when(baseProperty.getPropertyType()).thenReturn(PropertyType.APARTMENT);
        when(baseProperty.getBedrooms()).thenReturn(2);
        when(baseProperty.getBathrooms()).thenReturn(new BigDecimal("2"));
        when(baseProperty.getLocation()).thenReturn(location);
        when(baseProperty.hasCoordinates()).thenReturn(true);
        when(baseProperty.getLat()).thenReturn(-25.2637);
        when(baseProperty.getLng()).thenReturn(-57.5759);

        nearbyProperty1 = mock(Property.class);
        when(nearbyProperty1.getId()).thenReturn(2L);
        when(nearbyProperty1.getPrice()).thenReturn(new BigDecimal("155000"));
        when(nearbyProperty1.getBedrooms()).thenReturn(2);
        when(nearbyProperty1.getBathrooms()).thenReturn(new BigDecimal("2"));
        when(nearbyProperty1.hasCoordinates()).thenReturn(false);

        nearbyProperty2 = mock(Property.class);
        when(nearbyProperty2.getId()).thenReturn(3L);
        when(nearbyProperty2.getPrice()).thenReturn(new BigDecimal("145000"));
        when(nearbyProperty2.getBedrooms()).thenReturn(3);
        when(nearbyProperty2.getBathrooms()).thenReturn(new BigDecimal("2.5"));
        when(nearbyProperty2.hasCoordinates()).thenReturn(false);

        summaryResponse1 = createSummaryResponse("Nearby Similar Apartment");
        summaryResponse2 = createSummaryResponse("Nearby Different Apartment");
    }

    @Test
    void findSimilarProperties_withValidLimit_returnsProperties() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(baseProperty));

        doReturn(Arrays.asList(nearbyProperty1, nearbyProperty2))
                .when(propertyRepository)
                .findNearbyProperties(
                        anyLong(), anyDouble(), anyDouble(),
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                        anyString(), any(), any(), any(), anyDouble(), anyInt()
                );

        // toSummaryResponse en vez de toResponse
        when(propertyMapper.toSummaryResponse(nearbyProperty1)).thenReturn(summaryResponse1);
        when(propertyMapper.toSummaryResponse(nearbyProperty2)).thenReturn(summaryResponse2);

        List<PropertySummaryResponse> result = propertyService.findSimilarProperties(PROPERTY_ID, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void findSimilarProperties_withInvalidLimit_throwsBadRequestException() {
        assertThrows(BadRequestException.class,
                () -> propertyService.findSimilarProperties(PROPERTY_ID, 0));
        assertThrows(BadRequestException.class,
                () -> propertyService.findSimilarProperties(PROPERTY_ID, 21));
    }

    @Test
    void findSimilarProperties_withNonExistentProperty_throwsResourceNotFoundException() {
        when(propertyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> propertyService.findSimilarProperties(999L, 5));
    }

    @Test
    void findSimilarProperties_withPropertyWithoutCoordinates_usesFallbackSearch() {
        when(baseProperty.hasCoordinates()).thenReturn(false);
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(baseProperty));

        doReturn(Arrays.asList(nearbyProperty1, nearbyProperty2))
                .when(propertyRepository)
                .findByCity(
                        anyLong(), anyString(), anyString(),
                        any(), any(), any(), anyInt()
                );

        // toSummaryResponse en vez de toResponse
        when(propertyMapper.toSummaryResponse(nearbyProperty1)).thenReturn(summaryResponse1);
        when(propertyMapper.toSummaryResponse(nearbyProperty2)).thenReturn(summaryResponse2);

        List<PropertySummaryResponse> result = propertyService.findSimilarProperties(PROPERTY_ID, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
