package com.openroof.openroof.service;

import com.openroof.openroof.common.embeddable.RequestMetadata;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyView;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.ExteriorFeatureRepository;
import com.openroof.openroof.repository.HighlightRepository;
import com.openroof.openroof.repository.InteriorFeatureRepository;
import com.openroof.openroof.repository.LocationRepository;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.PropertyViewRepository;
import com.openroof.openroof.repository.UserPreferenceRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyViewServiceTest {

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
    @Mock private jakarta.servlet.http.HttpServletRequest request;
    @Mock private jakarta.servlet.http.HttpSession session;

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
                highlightRepository,
                paymentRepository,
                propertyMapper,
                notificationService,
                auditService,
                userPreferenceRepository,
                propertyRelevanceService);
    }

    @Test
    void registerView_persistsViewAndUpdatesCount() {
        Property property = property();
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));
        when(propertyViewRepository.save(any(PropertyView.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(propertyRepository.incrementViewCount(10L)).thenReturn(1);
        Property updatedProperty = property();
        updatedProperty.setViewCount(7);
        AtomicInteger findByIdCallCount = new AtomicInteger(0);
        when(propertyRepository.findById(10L)).thenAnswer(inv -> {
            int call = findByIdCallCount.getAndIncrement();
            return call == 0 ? Optional.of(property) : Optional.of(updatedProperty);
        });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(" 10.0.0.1 , 10.0.0.2");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getHeader("Referer")).thenReturn("https://example.com");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("session-123");

        long count = propertyService.registerView(10L, null, request);

        assertEquals(7L, count);

        ArgumentCaptor<PropertyView> captor = ArgumentCaptor.forClass(PropertyView.class);
        verify(propertyViewRepository).save(captor.capture());
        PropertyView saved = captor.getValue();
        RequestMetadata metadata = saved.getRequestMetadata();
        assertEquals("10.0.0.1", metadata.getIpAddress());
        assertEquals("JUnit", metadata.getUserAgent());
        assertEquals("https://example.com", saved.getReferrer());
        assertEquals("session-123", saved.getSessionId());
        verify(propertyRepository).incrementViewCount(10L);
        verify(propertyViewRepository, never()).countByProperty_Id(eq(10L));
    }

    @Test
    void getViewCount_returnsStoredCount() {
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property()));
        when(propertyViewRepository.countByProperty_Id(10L)).thenReturn(4L);

        long count = propertyService.getViewCount(10L);

        assertEquals(4L, count);
    }

    @Test
    void getViewCount_missingPropertyThrowsNotFound() {
        when(propertyRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(com.openroof.openroof.exception.ResourceNotFoundException.class,
                () -> propertyService.getViewCount(10L));
    }

    private Property property() {
        User owner = User.builder()
                .email("owner@test.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .name("Owner")
                .build();
        owner.setId(1L);

        Property property = Property.builder()
                .title("Casa")
                .propertyType(PropertyType.HOUSE)
                .address("Calle 1")
                .price(BigDecimal.valueOf(100000))
                .owner(owner)
                .build();
        property.setId(10L);
        return property;
    }
}
