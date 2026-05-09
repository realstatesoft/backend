package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyView;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

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


    @BeforeEach
    void setUp() {
    }

    @InjectMocks
    private PropertyViewService propertyViewService;

    @Test
    @DisplayName("Registrar vista reciente crea una nueva vista cuando la propiedad existe")
    void registerRecentView_createsView() {
        User user = user(1L);
        Property property = property(10L, "Casa 1");
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));
        when(propertyViewRepository.findFirstByUser_IdAndProperty_IdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(Optional.empty());

        propertyViewService.registerRecentView(10L, user);

        verify(propertyViewRepository).save(argThat(view ->
                view.getUser().getId().equals(1L) && view.getProperty().getId().equals(10L)));
    }

    @Test
    @DisplayName("Registrar vista reciente reemplaza una vista previa de la misma propiedad")
    void registerRecentView_replacesExistingView() {
        User user = user(1L);
        Property property = property(10L, "Casa 1");
        PropertyView existing = PropertyView.builder().property(property).user(user).build();
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));
        when(propertyViewRepository.findFirstByUser_IdAndProperty_IdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(Optional.of(existing));

        propertyViewService.registerRecentView(10L, user);

        verify(propertyViewRepository).delete(existing);
        verify(propertyViewRepository).save(any(PropertyView.class));
    }

    @Test
    @DisplayName("Registrar vista de propiedad inexistente lanza excepción")
    void registerRecentView_missingProperty_throws() {
        User user = user(1L);
        when(propertyRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyViewService.registerRecentView(10L, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Propiedad no encontrada");
    }

    @Test
    @DisplayName("Obtener propiedades recientes devuelve únicas y ordenadas por recencia")
    void getRecentProperties_returnsOrderedUniqueProperties() {
        User user = user(1L);
        Property p1 = property(10L, "Casa 1");
        Property p2 = property(20L, "Casa 2");
        Property p3 = property(30L, "Casa 3");
        PageRequest recentPage = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        PropertyView v1 = view(p1, user, LocalDateTime.of(2026, 4, 21, 12, 0));
        PropertyView v2 = view(p2, user, LocalDateTime.of(2026, 4, 21, 11, 0));
        PropertyView v3 = view(p1, user, LocalDateTime.of(2026, 4, 21, 10, 0));
        PropertyView v4 = view(p3, user, LocalDateTime.of(2026, 4, 21, 9, 0));

        when(propertyViewRepository.findRecentByUserId(1L, recentPage)).thenReturn(List.of(v1, v2, v3, v4));
        when(propertyMapper.toSummaryResponse(p1)).thenReturn(summary(10L, "Casa 1"));
        when(propertyMapper.toSummaryResponse(p2)).thenReturn(summary(20L, "Casa 2"));
        when(propertyMapper.toSummaryResponse(p3)).thenReturn(summary(30L, "Casa 3"));

        List<PropertySummaryResponse> recent = propertyViewService.getRecentProperties(1L);

        assertThat(recent).extracting(PropertySummaryResponse::id)
                .containsExactly(10L, 20L, 30L);
    }

    private static User user(Long id) {
        User user = User.builder()
                .email("user@test.com")
                .name("User Test")
                .build();
        user.setId(id);
        return user;
    }

    private static Property property(Long id, String title) {
        Property property = Property.builder().title(title).build();
        property.setId(id);
        return property;
    }

    private static PropertyView view(Property property, User user, LocalDateTime createdAt) {
        PropertyView view = PropertyView.builder()
                .property(property)
                .user(user)
                .build();
        view.setCreatedAt(createdAt);
        return view;
    }

    private PropertySummaryResponse summary(Long id, String title) {
        return new PropertySummaryResponse(
                id,                 // id
                title,              // title
                new BigDecimal("150000"),         // price
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
}
