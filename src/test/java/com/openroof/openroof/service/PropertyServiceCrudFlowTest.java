package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.CreatePropertyRequest;
import com.openroof.openroof.dto.property.PropertyResponse;
import com.openroof.openroof.dto.property.UpdatePropertyRequest;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.property.Property;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceCrudFlowTest {

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
    private HighlightRepository highlightRepository;
    @Mock
    private PaymentRepository paymentRepository;
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
    @Mock
    private EntityManager entityManager;

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
                propertyRelevanceService,
                entityManager
        );
    }

    @Test
    void createUpdateDelete_propertyFlow_success() {
        Long propertyId = 10L;
        Long ownerId = 1L;
        Long agentId = 2L;

        User owner = User.builder().email("owner@test.com").passwordHash("x").name("Owner").build();
        owner.setId(ownerId);

        User agentUser = User.builder().email("agent@test.com").passwordHash("x").name("Agent").build();
        agentUser.setId(20L);
        AgentProfile agentProfile = AgentProfile.builder().user(agentUser).build();
        agentProfile.setId(agentId);

        Property property = Property.builder()
                .title("Casa inicial")
                .propertyType(PropertyType.HOUSE)
                .address("Calle 1")
                .price(BigDecimal.valueOf(100000))
                .owner(owner)
                .agent(agentProfile)
                .rooms(List.of())
                .media(List.of())
                .exteriorFeatures(List.of())
                .build();
        property.setId(propertyId);

        CreatePropertyRequest createRequest = new CreatePropertyRequest(
                "Casa inicial",
                "Descripcion",
                PropertyType.HOUSE,
                null, // category
                null, // listingType (new)
                null, // rentAmount (new)
                null, // rentCurrency (new)
                null, // rentFrequency (new)
                null, // rentBillingCycle (new)
                "Calle 1",
                null,
                null,
                null,
                BigDecimal.valueOf(100000),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ownerId,
                agentId,
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
                null
        );

        UpdatePropertyRequest updateRequest = new UpdatePropertyRequest(
                "Casa modificada",
                null,
                null,
                null, // category
                null, // listingType (new)
                null, // rentAmount (new)
                null, // rentCurrency (new)
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
                null,
                null,
                null,
                null
        );

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.getReferenceById(ownerId)).thenReturn(owner);
        when(agentProfileRepository.findById(agentId)).thenReturn(Optional.of(agentProfile));
        when(propertyMapper.toEntity(createRequest)).thenReturn(property);
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            Property target = invocation.getArgument(0);
            UpdatePropertyRequest req = invocation.getArgument(1);
            target.setTitle(req.title());
            return null;
        }).when(propertyMapper).updateEntity(any(Property.class), eq(updateRequest));

        when(propertyMapper.toResponse(any(Property.class))).thenAnswer(invocation -> {
            Property p = invocation.getArgument(0);
            return simpleResponse(p.getId(), p.getTitle(), p.getOwner().getId());
        });

        PropertyResponse created = propertyService.create(createRequest, owner);
        assertNotNull(created);
        assertNotNull(created.id());

        PropertyResponse updated = propertyService.update(propertyId, updateRequest, ownerId, UserRole.USER);
        assertNotNull(updated);
        assertTrue(updated.title().contains("modificada"));

        propertyService.delete(propertyId, ownerId, UserRole.USER);
        assertNotNull(property.getDeletedAt());

        verify(propertyRepository, times(4)).save(any(Property.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void search_withCompletedPreferencesPaginatesRelevanceInDatabase() {
        Long userId = 99L;
        UserPreference preference = UserPreference.builder()
                .onboardingCompleted(true)
                .build();

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Property> query = mock(CriteriaQuery.class);
        Root<Property> root = mock(Root.class);
        Predicate predicate = mock(Predicate.class);
        Expression<Integer> score = mock(Expression.class);
        Path<Object> idPath = mock(Path.class);
        Order scoreOrder = mock(Order.class);
        Order idOrder = mock(Order.class);
        TypedQuery<Property> typedQuery = mock(TypedQuery.class);

        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Property.class)).thenReturn(query);
        when(query.from(Property.class)).thenReturn(root);
        when(cb.conjunction()).thenReturn(predicate);
        when(query.where(predicate)).thenReturn(query);
        when(cb.literal(0)).thenReturn(score);
        when(root.get("id")).thenReturn(idPath);
        when(cb.desc(score)).thenReturn(scoreOrder);
        when(cb.desc(idPath)).thenReturn(idOrder);
        when(query.select(root)).thenReturn(query);
        when(query.orderBy(scoreOrder, idOrder)).thenReturn(query);
        when(entityManager.createQuery(query)).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(20)).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(10)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());
        when(propertyRepository.count(any(Specification.class))).thenReturn(25L);

        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        propertyService.search(null, pageable, userId);

        verify(typedQuery).setFirstResult(20);
        verify(typedQuery).setMaxResults(10);
        verify(propertyRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    private PropertyResponse simpleResponse(Long id, String title, Long ownerId) {
        return new PropertyResponse(
                id,
                title,
                null,
                "HOUSE",
                null,
                "Calle 1",
                null,
                null,
                BigDecimal.ONE,
                "SALE",
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "PENDING",
                null,
                null,
                false,
                null,
                0,
                0,
                ownerId,
                "Owner",
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null, null
        );
    }
}
