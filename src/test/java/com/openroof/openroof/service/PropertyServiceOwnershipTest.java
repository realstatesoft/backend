package com.openroof.openroof.service;

import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.ExteriorFeatureRepository;
import com.openroof.openroof.repository.InteriorFeatureRepository;
import com.openroof.openroof.repository.LocationRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserPreferenceRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PropertyService.checkOwnership() and status-change ADMIN enforcement.
 */
@ExtendWith(MockitoExtension.class)
class PropertyServiceOwnershipTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private ExteriorFeatureRepository exteriorFeatureRepository;
    @Mock private InteriorFeatureRepository interiorFeatureRepository;
    @Mock private PropertyMapper propertyMapper;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private PropertyRelevanceService propertyRelevanceService;

    private PropertyService propertyService;

    private static final Long PROPERTY_ID = 10L;
    private static final Long OWNER_ID    = 1L;
    private static final Long OTHER_ID    = 2L;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(
                propertyRepository, userRepository, locationRepository,
                agentProfileRepository, exteriorFeatureRepository,
                interiorFeatureRepository, propertyMapper, notificationService,
                userPreferenceRepository, propertyRelevanceService, auditService);
    }

    // ─── checkOwnership via delete() ──────────────────────────────

    @Test
    void delete_adminBypassesOwnershipCheck() {
        // ADMIN should NOT even need the property to exist for the ownership gate
        // (property is loaded later by findPropertyOrThrow, so we still need to stub it)
        Property property = property(OWNER_ID);
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

        when(userRepository.getReferenceById(OTHER_ID)).thenAnswer(inv -> {
            User u = User.builder().email("other@test.com").passwordHash("x").role(UserRole.ADMIN).build();
            u.setId(OTHER_ID);
            return u;
        });
        assertDoesNotThrow(() -> propertyService.delete(PROPERTY_ID, OTHER_ID, UserRole.ADMIN));
    }

    @Test
    void delete_ownerCanDelete() {
        Property property = property(OWNER_ID);
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

        when(userRepository.getReferenceById(OWNER_ID)).thenAnswer(inv -> {
            User u = User.builder().email("owner@test.com").passwordHash("x").role(UserRole.USER).build();
            u.setId(OWNER_ID);
            return u;
        });
        assertDoesNotThrow(() -> propertyService.delete(PROPERTY_ID, OWNER_ID, UserRole.USER));
    }

    @Test
    void delete_nonOwnerThrowsForbidden() {
        Property property = property(OWNER_ID);
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

        assertThrows(ForbiddenException.class,
                () -> propertyService.delete(PROPERTY_ID, OTHER_ID, UserRole.USER));
    }

    @Test
    void delete_missingPropertyThrowsNotFound() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> propertyService.delete(PROPERTY_ID, OWNER_ID, UserRole.USER));
    }

    // ─── changeStatus ADMIN-only ───────────────────────────────────

    @Test
    void changeStatus_adminCanChange() {
        Property property = property(OWNER_ID);
        property.setStatus(PropertyStatus.PENDING);
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(propertyRepository.save(property)).thenReturn(property);
        when(propertyMapper.toResponse(property)).thenReturn(null);

        User admin = User.builder().email("admin@test.com").passwordHash("x").role(UserRole.ADMIN).build();
        admin.setId(99L);
        assertDoesNotThrow(() ->
                propertyService.changeStatus(PROPERTY_ID, PropertyStatus.APPROVED, admin));
    }

    @Test
    void changeStatus_userThrowsForbidden() {
        User user = User.builder().email("u@test.com").passwordHash("x").role(UserRole.USER).build();
        user.setId(5L);
        assertThrows(ForbiddenException.class,
                () -> propertyService.changeStatus(PROPERTY_ID, PropertyStatus.APPROVED, user));
    }

    @Test
    void changeStatus_agentThrowsForbidden() {
        User agent = User.builder().email("a@test.com").passwordHash("x").role(UserRole.AGENT).build();
        agent.setId(6L);
        assertThrows(ForbiddenException.class,
                () -> propertyService.changeStatus(PROPERTY_ID, PropertyStatus.APPROVED, agent));
    }

    // ─── clearTrashcan ownership ───────────────────────────────────

    @Test
    void clearTrashcan_userCanClearOwnTrash() {
        when(propertyRepository.clearTrashcanByOwner(
                org.mockito.ArgumentMatchers.eq(OWNER_ID),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(3);

        when(userRepository.getReferenceById(OWNER_ID)).thenAnswer(inv -> {
            User u = User.builder().email("owner@test.com").passwordHash("x").role(UserRole.USER).build();
            u.setId(OWNER_ID);
            return u;
        });
        assertDoesNotThrow(() ->
                propertyService.clearTrashcanForUser(OWNER_ID, OWNER_ID, UserRole.USER));
    }

    @Test
    void clearTrashcan_userCannotClearOthersTrash() {
        assertThrows(ForbiddenException.class,
                () -> propertyService.clearTrashcanForUser(OWNER_ID, OTHER_ID, UserRole.USER));
    }

    @Test
    void clearTrashcan_adminCanClearAnyTrash() {
        when(propertyRepository.clearTrashcanByOwner(
                org.mockito.ArgumentMatchers.eq(OWNER_ID),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(2);

        when(userRepository.getReferenceById(OTHER_ID)).thenAnswer(inv -> {
            User u = User.builder().email("admin@test.com").passwordHash("x").role(UserRole.ADMIN).build();
            u.setId(OTHER_ID);
            return u;
        });
        assertDoesNotThrow(() ->
                propertyService.clearTrashcanForUser(OWNER_ID, OTHER_ID, UserRole.ADMIN));
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private Property property(Long ownerId) {
        User owner = User.builder()
                .email("owner@test.com")
                .passwordHash("x")
                .role(UserRole.USER)
                .build();
        owner.setId(ownerId);

        Property p = Property.builder()
                .title("Test")
                .propertyType(PropertyType.HOUSE)
                .address("Calle 1")
                .price(BigDecimal.valueOf(100_000))
                .owner(owner)
                .build();
        p.setId(PROPERTY_ID);
        return p;
    }
}
