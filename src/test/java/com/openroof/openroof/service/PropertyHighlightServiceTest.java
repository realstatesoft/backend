package com.openroof.openroof.service;

import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.property.Highlight;
import com.openroof.openroof.model.property.Property;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyHighlightServiceTest {

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

    private static final Long PROPERTY_ID = 1L;

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
    }

    private Property buildProperty(Long id) {
        Property p = Property.builder()
                .title("Casa de prueba")
                .propertyType(PropertyType.HOUSE)
                .address("Calle 1")
                .price(BigDecimal.valueOf(100_000))
                .build();
        p.setId(id);
        return p;
    }

    private Highlight buildActiveHighlight(Property property, int daysLeft) {
        LocalDateTime now = LocalDateTime.now();
        Highlight h = Highlight.builder()
                .property(property)
                .highlightedFrom(now.minusDays(1))
                .highlightedUntil(now.plusDays(daysLeft))
                .build();
        h.setId(10L);
        return h;
    }

    // ─── highlightProperty ────────────────────────────────────────

    @Nested
    @DisplayName("highlightProperty()")
    class HighlightProperty {

        @Test
        @DisplayName("Crea un nuevo highlight cuando no hay uno activo")
        void createsNewHighlightWhenNoneActive() {
            Property property = buildProperty(PROPERTY_ID);
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(highlightRepository
                    .findFirstByProperty_IdAndHighlightedUntilAfterOrderByHighlightedUntilDesc(
                            eq(PROPERTY_ID), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());
            when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));

            propertyService.highlightProperty(PROPERTY_ID, 7);

            ArgumentCaptor<Highlight> captor = ArgumentCaptor.forClass(Highlight.class);
            verify(highlightRepository).save(captor.capture());
            Highlight saved = captor.getValue();
            assertThat(saved.getProperty()).isEqualTo(property);
            assertThat(saved.getPayment()).isNull();
            assertThat(saved.getHighlightedFrom()).isNotNull();
            assertThat(saved.getHighlightedUntil())
                    .isAfterOrEqualTo(LocalDateTime.now().plusDays(6));
        }

        @Test
        @DisplayName("Extiende el highlight activo existente")
        void extendsExistingActiveHighlight() {
            Property property = buildProperty(PROPERTY_ID);
            Highlight existing = buildActiveHighlight(property, 3);
            LocalDateTime originalUntil = existing.getHighlightedUntil();

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(highlightRepository
                    .findFirstByProperty_IdAndHighlightedUntilAfterOrderByHighlightedUntilDesc(
                            eq(PROPERTY_ID), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(existing));
            when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));

            propertyService.highlightProperty(PROPERTY_ID, 7);

            ArgumentCaptor<Highlight> captor = ArgumentCaptor.forClass(Highlight.class);
            verify(highlightRepository).save(captor.capture());
            assertThat(captor.getValue().getHighlightedUntil())
                    .isEqualTo(originalUntil.plusDays(7));
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la propiedad no existe")
        void throwsWhenPropertyNotFound() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.highlightProperty(99L, 7))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(highlightRepository, never()).save(any());
        }
    }

    // ─── highlightPropertyWithPayment ─────────────────────────────

    @Nested
    @DisplayName("highlightPropertyWithPayment()")
    class HighlightPropertyWithPayment {

        @Test
        @DisplayName("Crea un nuevo highlight con referencia al pago")
        void createsNewHighlightWithPayment() {
            Property property = buildProperty(PROPERTY_ID);
            Payment payment = Payment.builder().status(PaymentStatus.APPROVED).build();
            payment.setId(5L);

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
            when(highlightRepository
                    .findFirstByProperty_IdAndHighlightedUntilAfterOrderByHighlightedUntilDesc(
                            eq(PROPERTY_ID), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());
            when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));

            propertyService.highlightPropertyWithPayment(PROPERTY_ID, 5L, 30);

            ArgumentCaptor<Highlight> captor = ArgumentCaptor.forClass(Highlight.class);
            verify(highlightRepository).save(captor.capture());
            Highlight saved = captor.getValue();
            assertThat(saved.getPayment()).isEqualTo(payment);
            assertThat(saved.getHighlightedUntil())
                    .isAfterOrEqualTo(LocalDateTime.now().plusDays(29));
        }

        @Test
        @DisplayName("Extiende highlight activo y actualiza la referencia al pago")
        void extendsExistingHighlightAndUpdatesPayment() {
            Property property = buildProperty(PROPERTY_ID);
            Highlight existing = buildActiveHighlight(property, 5);
            LocalDateTime originalUntil = existing.getHighlightedUntil();
            Payment payment = Payment.builder().status(PaymentStatus.APPROVED).build();
            payment.setId(7L);

            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));
            when(highlightRepository
                    .findFirstByProperty_IdAndHighlightedUntilAfterOrderByHighlightedUntilDesc(
                            eq(PROPERTY_ID), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(existing));
            when(highlightRepository.save(any(Highlight.class))).thenAnswer(inv -> inv.getArgument(0));

            propertyService.highlightPropertyWithPayment(PROPERTY_ID, 7L, 10);

            ArgumentCaptor<Highlight> captor = ArgumentCaptor.forClass(Highlight.class);
            verify(highlightRepository).save(captor.capture());
            assertThat(captor.getValue().getHighlightedUntil())
                    .isEqualTo(originalUntil.plusDays(10));
            assertThat(captor.getValue().getPayment()).isEqualTo(payment);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la propiedad no existe")
        void throwsWhenPropertyNotFound() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.highlightPropertyWithPayment(99L, 5L, 7))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(highlightRepository, never()).save(any());
        }
    }

    // ─── cleanExpiredHighlights ───────────────────────────────────

    @Nested
    @DisplayName("cleanExpiredHighlights()")
    class CleanExpiredHighlights {

        @Test
        @DisplayName("Delega la desactivación al repositorio con la fecha actual")
        void delegatesToRepositoryWithCurrentTime() {
            propertyService.cleanExpiredHighlights();

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(highlightRepository).deactivateExpired(captor.capture());
            assertThat(captor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}
