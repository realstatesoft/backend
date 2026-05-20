package com.openroof.openroof.scheduler;

import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.property.Location;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.search.SearchPreference;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AlertRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.SearchPreferenceRepository;
import com.openroof.openroof.repository.SystemConfigRepository;
import com.openroof.openroof.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PropertyAlertScheduler")
class PropertyAlertSchedulerTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private SearchPreferenceRepository searchPreferenceRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private EmailService emailService;
    @Mock private SystemConfigRepository systemConfigRepository;

    @InjectMocks private PropertyAlertScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Watermark not configured → defaults to 1 hour ago
        when(systemConfigRepository.findByConfigKey(any())).thenReturn(Optional.empty());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Builds a property whose owner has the given id (owner != preference user). */
    private Property propertyWithOwner(long ownerId) {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        return Property.builder()
                .propertyType(PropertyType.HOUSE)
                .category(PropertyCategory.SALE)
                .price(BigDecimal.valueOf(200_000))
                .bedrooms(3)
                .address("Av. Siempreviva 742")
                .owner(owner)
                .build();
    }

    /** Builds a mocked SearchPreference for a given userId and filter map. */
    private SearchPreference prefWithFilters(long userId, Map<String, Object> filters) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user" + userId + "@test.com");
        when(user.getName()).thenReturn("User " + userId);

        SearchPreference pref = mock(SearchPreference.class);
        when(pref.getId()).thenReturn(userId * 10);
        when(pref.getUser()).thenReturn(user);
        when(pref.getFilters()).thenReturn(filters);
        return pref;
    }

    // ─── processPropertyAlerts ────────────────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_noNewProperties_updatesWatermarkOnly")
    void processPropertyAlerts_noNewProperties_updatesWatermarkOnly() {
        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of());

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
        verify(systemConfigRepository).save(any());
    }

    @Test
    @DisplayName("processPropertyAlerts_noActivePreferences_noAlertsCreated")
    void processPropertyAlerts_noActivePreferences_noAlertsCreated() {
        Property prop = propertyWithOwner(99L);
        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of());

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    @Test
    @DisplayName("processPropertyAlerts_ownerSameAsPreferenceUser_skipsMatch")
    void processPropertyAlerts_ownerSameAsPreferenceUser_skipsMatch() {
        long sameId = 5L;
        Property prop = propertyWithOwner(sameId);
        SearchPreference pref = prefWithFilters(sameId, new HashMap<>(Map.of("propertyType", "HOUSE")));
        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    // ─── matches – null / empty filters ──────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_nullFilters_noMatch")
    void processPropertyAlerts_nullFilters_noMatch() {
        Property prop = propertyWithOwner(99L);
        SearchPreference pref = prefWithFilters(1L, null);
        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    @Test
    @DisplayName("processPropertyAlerts_emptyFilters_noMatch")
    void processPropertyAlerts_emptyFilters_noMatch() {
        Property prop = propertyWithOwner(99L);
        SearchPreference pref = prefWithFilters(1L, new HashMap<>());
        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    // ─── matches – propertyType filter ───────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_propertyTypeMismatch_noMatch")
    void processPropertyAlerts_propertyTypeMismatch_noMatch() {
        // Property is HOUSE; filter requests APARTMENT → no match
        Map<String, Object> filters = new HashMap<>();
        filters.put("propertyType", "APARTMENT");
        Property prop = propertyWithOwner(99L);
        SearchPreference pref = prefWithFilters(1L, filters);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    @Test
    @DisplayName("processPropertyAlerts_spanishTypeCasa_matchesHouseProperty")
    void processPropertyAlerts_spanishTypeCasa_matchesHouseProperty() {
        // "casa" is translated to "HOUSE" → property IS HOUSE → match
        // duplicate check returns true → no save (avoids TransactionSynchronizationManager)
        Map<String, Object> filters = new HashMap<>();
        filters.put("propertyType", "casa");
        Property prop = propertyWithOwner(99L);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        SearchPreference pref = prefWithFilters(1L, filters);
        when(searchPreferenceRepository.findByNotificationsEnabledTrue()).thenReturn(List.of(pref));
        when(alertRepository.existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any()))
                .thenReturn(true);

        scheduler.processPropertyAlerts();

        verify(alertRepository).existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
        verify(alertRepository, never()).save(any());
    }

    // ─── matches – category filter ────────────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_spanishCategoryVenta_matchesSaleProperty")
    void processPropertyAlerts_spanishCategoryVenta_matchesSaleProperty() {
        // "venta" is translated to "SALE" → property category IS SALE → match
        Map<String, Object> filters = new HashMap<>();
        filters.put("category", "venta");
        Property prop = propertyWithOwner(99L); // SALE by default

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        SearchPreference pref = prefWithFilters(1L, filters);
        when(searchPreferenceRepository.findByNotificationsEnabledTrue()).thenReturn(List.of(pref));
        when(alertRepository.existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any()))
                .thenReturn(true);

        scheduler.processPropertyAlerts();

        verify(alertRepository).existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    // ─── matches – price filter ───────────────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_priceBelowMinBudget_noMatch")
    void processPropertyAlerts_priceBelowMinBudget_noMatch() {
        // property price 50_000, filter minPrice 100_000 → no match
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Property cheapProp = Property.builder()
                .propertyType(PropertyType.HOUSE).category(PropertyCategory.SALE)
                .price(BigDecimal.valueOf(50_000)).bedrooms(2).address("Addr").owner(owner)
                .build();

        Map<String, Object> filters = new HashMap<>();
        filters.put("minPrice", "100000");
        SearchPreference pref = prefWithFilters(1L, filters);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(cheapProp));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    @Test
    @DisplayName("processPropertyAlerts_priceAboveMaxBudget_noMatch")
    void processPropertyAlerts_priceAboveMaxBudget_noMatch() {
        // property price 200_000, filter maxPrice 100_000 → no match
        Map<String, Object> filters = new HashMap<>();
        filters.put("maxPrice", "100000");
        Property prop = propertyWithOwner(99L);
        SearchPreference pref = prefWithFilters(1L, filters);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    // ─── matches – bedrooms filter ────────────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_bedroomsLessThanMin_noMatch")
    void processPropertyAlerts_bedroomsLessThanMin_noMatch() {
        // property has 1 bedroom, filter requests 3+ → no match
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Property fewBedrooms = Property.builder()
                .propertyType(PropertyType.HOUSE).category(PropertyCategory.SALE)
                .price(BigDecimal.valueOf(200_000)).bedrooms(1).address("Addr").owner(owner)
                .build();

        Map<String, Object> filters = new HashMap<>();
        filters.put("minBedrooms", "3");
        SearchPreference pref = prefWithFilters(1L, filters);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(fewBedrooms));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    // ─── matches – city filter ────────────────────────────────────────────────

    @Test
    @DisplayName("processPropertyAlerts_cityMismatch_noMatch")
    void processPropertyAlerts_cityMismatch_noMatch() {
        Location location = mock(Location.class);
        when(location.getCity()).thenReturn("Rosario");

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE).category(PropertyCategory.SALE)
                .price(BigDecimal.valueOf(200_000)).bedrooms(2).address("Addr")
                .owner(owner).location(location).build();

        Map<String, Object> filters = new HashMap<>();
        filters.put("city", "Buenos Aires");
        SearchPreference pref = prefWithFilters(1L, filters);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    @Test
    @DisplayName("processPropertyAlerts_cityAccentsNormalized_matchesAfterNormalization")
    void processPropertyAlerts_cityAccentsNormalized_matchesAfterNormalization() {
        // "córdoba" normalizes to "cordoba", property city "Cordoba" → also "cordoba" → match
        Location location = mock(Location.class);
        when(location.getCity()).thenReturn("Cordoba");

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE).category(PropertyCategory.SALE)
                .price(BigDecimal.valueOf(200_000)).bedrooms(2).address("Addr")
                .owner(owner).location(location).build();

        Map<String, Object> filters = new HashMap<>();
        filters.put("city", "córdoba");

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        SearchPreference pref = prefWithFilters(1L, filters);
        when(searchPreferenceRepository.findByNotificationsEnabledTrue()).thenReturn(List.of(pref));
        when(alertRepository.existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any()))
                .thenReturn(true);

        scheduler.processPropertyAlerts();

        verify(alertRepository).existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }

    @Test
    @DisplayName("processPropertyAlerts_propertyNullLocation_cityFilterRequires_noMatch")
    void processPropertyAlerts_propertyNullLocation_cityFilterRequires_noMatch() {
        // Property has no location; city filter is set → no match
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Property prop = Property.builder()
                .propertyType(PropertyType.HOUSE).category(PropertyCategory.SALE)
                .price(BigDecimal.valueOf(200_000)).bedrooms(2).address("Addr")
                .owner(owner).location(null).build();

        Map<String, Object> filters = new HashMap<>();
        filters.put("city", "Buenos Aires");
        SearchPreference pref = prefWithFilters(1L, filters);

        when(propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        any(), any(), any()))
                .thenReturn(List.of(prop));
        when(searchPreferenceRepository.findByNotificationsEnabledTrue())
                .thenReturn(List.of(pref));

        scheduler.processPropertyAlerts();

        verify(alertRepository, never())
                .existsByUserIdAndPropertyIdAndSearchPreferenceId(any(), any(), any());
    }
}
