package com.openroof.openroof.security;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertySecurityTest {

    @Mock
    private PropertyRepository propertyRepository;

    private PropertySecurity propertySecurity;

    @BeforeEach
    void setUp() {
        propertySecurity = new PropertySecurity(propertyRepository);
    }

    @Test
    void canModify_allowsAdminWithoutLoadingProperty() {
        User admin = user(1L, UserRole.ADMIN);

        boolean allowed = propertySecurity.canModify(10L, admin);

        assertTrue(allowed);
        verifyNoInteractions(propertyRepository);
    }

    @Test
    void canModify_allowsAssignedAgent() {
        User currentAgentUser = user(2L, UserRole.AGENT);
        Property property = propertyWithAgentAndOwner(2L, 99L);
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));

        boolean allowed = propertySecurity.canModify(10L, currentAgentUser);

        assertTrue(allowed);
    }

    @Test
    void canModify_deniesUnassignedAgent() {
        User currentAgentUser = user(2L, UserRole.AGENT);
        Property property = propertyWithAgentAndOwner(3L, 99L);
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));

        boolean allowed = propertySecurity.canModify(10L, currentAgentUser);

        assertFalse(allowed);
    }

    @Test
    void canModify_allowsOwnerUser() {
        User owner = user(7L, UserRole.USER);
        Property property = propertyWithAgentAndOwner(2L, 7L);
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));

        boolean allowed = propertySecurity.canModify(10L, owner);

        assertTrue(allowed);
    }

    @Test
    void canModify_deniesNonOwnerUser() {
        User otherUser = user(8L, UserRole.USER);
        Property property = propertyWithAgentAndOwner(2L, 7L);
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));

        boolean allowed = propertySecurity.canModify(10L, otherUser);

        assertFalse(allowed);
    }

    @Test
    void canModify_throwsWhenPropertyNotFound() {
        User owner = user(7L, UserRole.USER);
        when(propertyRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> propertySecurity.canModify(10L, owner));
    }

    private User user(Long id, UserRole role) {
        User user = User.builder()
                .email(role.name().toLowerCase() + "@test.com")
                .passwordHash("secret")
                .role(role)
                .build();
        user.setId(id);
        return user;
    }

    private Property propertyWithAgentAndOwner(Long agentUserId, Long ownerId) {
        User agentUser = user(agentUserId, UserRole.AGENT);
        AgentProfile agent = AgentProfile.builder().user(agentUser).build();
        User owner = user(ownerId, UserRole.USER);

        Property property = Property.builder()
                .title("T")
                .propertyType(com.openroof.openroof.model.enums.PropertyType.HOUSE)
                .address("A")
                .price(java.math.BigDecimal.ONE)
                .owner(owner)
                .agent(agent)
                .build();
        property.setId(10L);
        return property;
    }
}
