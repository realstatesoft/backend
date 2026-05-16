package com.openroof.openroof.security;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyAssignment;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyAssignmentRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseSecurityTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LANDLORD_ID = 2L;
    private static final Long TENANT_ID = 3L;
    private static final Long OTHER_ID = 4L;
    private static final Long LEASE_ID = 100L;
    private static final Long PROPERTY_ID = 200L;

    @Mock private LeaseRepository leaseRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertySecurity propertySecurity;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private PropertyAssignmentRepository propertyAssignmentRepository;

    private LeaseSecurity leaseSecurity;

    @BeforeEach
    void setUp() {
        leaseSecurity = new LeaseSecurity(
                leaseRepository, userRepository, propertySecurity,
                agentProfileRepository, propertyAssignmentRepository);
    }

    // ---------- assertLeaseAccess ----------

    @Test
    void assertLeaseAccess_admin_allowed_withoutLoadingLease() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(user(ADMIN_ID, UserRole.ADMIN)));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(ADMIN_ID, LEASE_ID));

        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_landlord_allowed() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    @Test
    void assertLeaseAccess_tenant_allowed() {
        when(userRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(user(TENANT_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(TENANT_ID, LEASE_ID));
    }

    @Test
    void assertLeaseAccess_outsider_throwsAccessDenied_withDescriptiveMessage() {
        when(userRepository.findById(OTHER_ID))
                .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(OTHER_ID, LEASE_ID));

        assertTrue(ex.getMessage().contains(String.valueOf(OTHER_ID)));
        assertTrue(ex.getMessage().contains(String.valueOf(LEASE_ID)));
        assertTrue(ex.getMessage().toLowerCase().contains("landlord"));
        assertTrue(ex.getMessage().toLowerCase().contains("tenant"));
    }

    @Test
    void assertLeaseAccess_leaseNotFound_throwsAccessDenied_notEntityNotFound() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("lease"));
        assertTrue(ex.getMessage().contains(String.valueOf(LEASE_ID)));
    }

    @Test
    void assertLeaseAccess_userNotFound_throwsAccessDenied_withoutTouchingLeaseRepo() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("usuario"));
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_nullUserId_throwsAccessDenied_withoutTouchingAnyRepo() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(null, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("autenticad"));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_nullLeaseId_throwsAccessDenied_withoutTouchingAnyRepo() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, null));

        assertTrue(ex.getMessage().toLowerCase().contains("lease"));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_landlordOnLeaseWithNullTenant_allowed() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        Lease l = lease(LANDLORD_ID, TENANT_ID);
        l.setPrimaryTenant(null);
        when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(l));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    @Test
    void assertLeaseAccess_tenantOnLeaseWithNullLandlord_allowed() {
        when(userRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(user(TENANT_ID, UserRole.USER)));
        Lease l = lease(LANDLORD_ID, TENANT_ID);
        l.setLandlord(null);
        when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(l));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(TENANT_ID, LEASE_ID));
    }

    // ---------- hasLeaseAccess ----------

    @Test
    void hasLeaseAccess_landlord_returnsTrue() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertTrue(leaseSecurity.hasLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    @Test
    void hasLeaseAccess_outsider_returnsFalse() {
        when(userRepository.findById(OTHER_ID))
                .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertFalse(leaseSecurity.hasLeaseAccess(OTHER_ID, LEASE_ID));
    }

    @Test
    void hasLeaseAccess_nullUserId_returnsFalse() {
        assertFalse(leaseSecurity.hasLeaseAccess(null, LEASE_ID));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void hasLeaseAccess_nullLeaseId_returnsFalse_withoutTouchingAnyRepo() {
        assertFalse(leaseSecurity.hasLeaseAccess(LANDLORD_ID, null));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void hasLeaseAccess_leaseNotFound_returnsFalse() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.empty());

        assertFalse(leaseSecurity.hasLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    // ---------- canManageLease ----------

    @Nested
    @DisplayName("canManageLease()")
    class CanManageLease {

        @Test
        @DisplayName("ADMIN retorna true sin consultar el lease")
        void admin_returnsTrueWithoutLoadingLease() {
            User admin = user(ADMIN_ID, UserRole.ADMIN);

            assertTrue(leaseSecurity.canManageLease(LEASE_ID, admin));
            verifyNoInteractions(leaseRepository);
        }

        @Test
        @DisplayName("Owner de la propiedad retorna true cuando propertySecurity lo permite")
        void propertyOwner_returnsTrue() {
            User owner = user(OTHER_ID, UserRole.USER);
            Lease lease = leaseWithProperty(PROPERTY_ID);
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(lease));
            when(propertySecurity.canModify(PROPERTY_ID, owner)).thenReturn(true);

            assertTrue(leaseSecurity.canManageLease(LEASE_ID, owner));
        }

        @Test
        @DisplayName("Agente asignado retorna true cuando propertySecurity lo permite")
        void assignedAgent_returnsTrue() {
            User agent = user(OTHER_ID, UserRole.AGENT);
            Lease lease = leaseWithProperty(PROPERTY_ID);
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(lease));
            when(propertySecurity.canModify(PROPERTY_ID, agent)).thenReturn(true);

            assertTrue(leaseSecurity.canManageLease(LEASE_ID, agent));
        }

        @Test
        @DisplayName("Usuario sin relación con la propiedad retorna false")
        void outsider_returnsFalse() {
            User outsider = user(OTHER_ID, UserRole.USER);
            Lease lease = leaseWithProperty(PROPERTY_ID);
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(lease));
            when(propertySecurity.canModify(PROPERTY_ID, outsider)).thenReturn(false);

            assertFalse(leaseSecurity.canManageLease(LEASE_ID, outsider));
        }

        @Test
        @DisplayName("Lease no encontrado retorna false")
        void leaseNotFound_returnsFalse() {
            User user = user(OTHER_ID, UserRole.USER);
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.empty());

            assertFalse(leaseSecurity.canManageLease(LEASE_ID, user));
            verifyNoInteractions(propertySecurity);
        }

        @Test
        @DisplayName("Property null en el lease retorna false")
        void leaseWithNullProperty_returnsFalse() {
            User user = user(OTHER_ID, UserRole.USER);
            Lease lease = lease(LANDLORD_ID, TENANT_ID);  // sin property
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(lease));

            assertFalse(leaseSecurity.canManageLease(LEASE_ID, user));
            verifyNoInteractions(propertySecurity);
        }
    }

    // ---------- assertInstallmentAccess ----------

    @Nested
    @DisplayName("assertInstallmentAccess()")
    class AssertInstallmentAccess {

        @Test
        @DisplayName("ADMIN pasa sin cargar el lease")
        void admin_allowed_withoutLoadingLease() {
            when(userRepository.findById(ADMIN_ID))
                    .thenReturn(Optional.of(user(ADMIN_ID, UserRole.ADMIN)));

            assertDoesNotThrow(() -> leaseSecurity.assertInstallmentAccess(ADMIN_ID, LEASE_ID));

            verifyNoInteractions(leaseRepository);
        }

        @Test
        @DisplayName("Landlord del lease tiene acceso")
        void landlord_allowed() {
            when(userRepository.findById(LANDLORD_ID))
                    .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
            when(leaseRepository.findById(LEASE_ID))
                    .thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));

            assertDoesNotThrow(() -> leaseSecurity.assertInstallmentAccess(LANDLORD_ID, LEASE_ID));
        }

        @Test
        @DisplayName("Primary tenant del lease tiene acceso")
        void tenant_allowed() {
            when(userRepository.findById(TENANT_ID))
                    .thenReturn(Optional.of(user(TENANT_ID, UserRole.USER)));
            when(leaseRepository.findById(LEASE_ID))
                    .thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));

            assertDoesNotThrow(() -> leaseSecurity.assertInstallmentAccess(TENANT_ID, LEASE_ID));
        }

        @Test
        @DisplayName("Agente con assignment ACCEPTED sobre la propiedad tiene acceso")
        void agentWithAcceptedAssignment_allowed() {
            User agent = user(OTHER_ID, UserRole.AGENT);
            AgentProfile profile = AgentProfile.builder().user(agent).build();
            profile.setId(50L);

            when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(agent));
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));
            when(agentProfileRepository.findByUser_Id(OTHER_ID)).thenReturn(Optional.of(profile));
            when(propertyAssignmentRepository.findActiveByPropertyAndAgent(
                    eq(PROPERTY_ID), eq(50L), anyList()))
                    .thenReturn(Optional.of(new PropertyAssignment()));

            assertDoesNotThrow(() -> leaseSecurity.assertInstallmentAccess(OTHER_ID, LEASE_ID));
        }

        @Test
        @DisplayName("Agente sin assignment activo recibe AccessDeniedException")
        void agentWithoutAssignment_throwsAccessDenied() {
            User agent = user(OTHER_ID, UserRole.AGENT);
            AgentProfile profile = AgentProfile.builder().user(agent).build();
            profile.setId(50L);

            when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(agent));
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));
            when(agentProfileRepository.findByUser_Id(OTHER_ID)).thenReturn(Optional.of(profile));
            when(propertyAssignmentRepository.findActiveByPropertyAndAgent(
                    eq(PROPERTY_ID), eq(50L), anyList()))
                    .thenReturn(Optional.empty());

            assertThrows(AccessDeniedException.class,
                    () -> leaseSecurity.assertInstallmentAccess(OTHER_ID, LEASE_ID));
        }

        @Test
        @DisplayName("Agente sin perfil de agente recibe AccessDeniedException")
        void agentWithNoProfile_throwsAccessDenied() {
            User agent = user(OTHER_ID, UserRole.AGENT);

            when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(agent));
            when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));
            when(agentProfileRepository.findByUser_Id(OTHER_ID)).thenReturn(Optional.empty());

            assertThrows(AccessDeniedException.class,
                    () -> leaseSecurity.assertInstallmentAccess(OTHER_ID, LEASE_ID));
        }

        @Test
        @DisplayName("Usuario sin relación con el lease recibe AccessDeniedException con sus IDs")
        void outsider_throwsAccessDenied_withIds() {
            when(userRepository.findById(OTHER_ID))
                    .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
            when(leaseRepository.findById(LEASE_ID))
                    .thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> leaseSecurity.assertInstallmentAccess(OTHER_ID, LEASE_ID));

            assertTrue(ex.getMessage().contains(String.valueOf(OTHER_ID)));
            assertTrue(ex.getMessage().contains(String.valueOf(LEASE_ID)));
        }

        @Test
        @DisplayName("userId null lanza AccessDeniedException sin tocar repositorios")
        void nullUserId_throwsAccessDenied() {
            assertThrows(AccessDeniedException.class,
                    () -> leaseSecurity.assertInstallmentAccess(null, LEASE_ID));

            verifyNoInteractions(userRepository, leaseRepository);
        }

        @Test
        @DisplayName("leaseId null lanza AccessDeniedException sin tocar repositorios")
        void nullLeaseId_throwsAccessDenied() {
            assertThrows(AccessDeniedException.class,
                    () -> leaseSecurity.assertInstallmentAccess(LANDLORD_ID, null));

            verifyNoInteractions(userRepository, leaseRepository);
        }
    }

    // ---------- hasInstallmentAccess ----------

    @Nested
    @DisplayName("hasInstallmentAccess()")
    class HasInstallmentAccess {

        @Test
        @DisplayName("Landlord retorna true")
        void landlord_returnsTrue() {
            when(userRepository.findById(LANDLORD_ID))
                    .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
            when(leaseRepository.findById(LEASE_ID))
                    .thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));

            assertTrue(leaseSecurity.hasInstallmentAccess(LANDLORD_ID, LEASE_ID));
        }

        @Test
        @DisplayName("Usuario sin relación retorna false")
        void outsider_returnsFalse() {
            when(userRepository.findById(OTHER_ID))
                    .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
            when(leaseRepository.findById(LEASE_ID))
                    .thenReturn(Optional.of(leaseWithProperty(PROPERTY_ID)));

            assertFalse(leaseSecurity.hasInstallmentAccess(OTHER_ID, LEASE_ID));
        }

        @Test
        @DisplayName("userId null retorna false sin tocar repositorios")
        void nullUserId_returnsFalse() {
            assertFalse(leaseSecurity.hasInstallmentAccess(null, LEASE_ID));
            verifyNoInteractions(userRepository, leaseRepository);
        }

        @Test
        @DisplayName("leaseId null retorna false sin tocar repositorios")
        void nullLeaseId_returnsFalse() {
            assertFalse(leaseSecurity.hasInstallmentAccess(LANDLORD_ID, null));
            verifyNoInteractions(userRepository, leaseRepository);
        }

        @Test
        @DisplayName("Lease no encontrado retorna false")
        void leaseNotFound_returnsFalse() {
            when(userRepository.findById(LANDLORD_ID))
                    .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
            when(leaseRepository.findById(LEASE_ID))
                    .thenReturn(Optional.empty());

            assertFalse(leaseSecurity.hasInstallmentAccess(LANDLORD_ID, LEASE_ID));
        }
    }

    // ---------- canCreateLease ----------

    @Nested
    @DisplayName("canCreateLease()")
    class CanCreateLease {

        @Test
        @DisplayName("propertyId null retorna false sin consultar propertySecurity")
        void nullPropertyId_returnsFalse() {
            assertFalse(leaseSecurity.canCreateLease(null, user(OTHER_ID, UserRole.USER)));
            verifyNoInteractions(propertySecurity);
        }

        @Test
        @DisplayName("principal no es User retorna false")
        void nonUserPrincipal_returnsFalse() {
            assertFalse(leaseSecurity.canCreateLease(PROPERTY_ID, "not-a-user"));
            verifyNoInteractions(propertySecurity);
        }

        @Test
        @DisplayName("principal null retorna false")
        void nullPrincipal_returnsFalse() {
            assertFalse(leaseSecurity.canCreateLease(PROPERTY_ID, null));
            verifyNoInteractions(propertySecurity);
        }

        @Test
        @DisplayName("ADMIN retorna true sin consultar propertySecurity")
        void admin_returnsTrue() {
            User admin = user(ADMIN_ID, UserRole.ADMIN);
            assertTrue(leaseSecurity.canCreateLease(PROPERTY_ID, admin));
            verifyNoInteractions(propertySecurity);
        }

        @Test
        @DisplayName("Delega en propertySecurity.canModify y propaga su resultado")
        void delegatesToPropertySecurity() {
            User owner = user(OTHER_ID, UserRole.USER);
            when(propertySecurity.canModify(PROPERTY_ID, owner)).thenReturn(true);
            assertTrue(leaseSecurity.canCreateLease(PROPERTY_ID, owner));

            when(propertySecurity.canModify(PROPERTY_ID, owner)).thenReturn(false);
            assertFalse(leaseSecurity.canCreateLease(PROPERTY_ID, owner));
        }
    }

    // ---------- helpers ----------

    private User user(Long id, UserRole role) {
        User u = User.builder()
                .email(role.name().toLowerCase() + id + "@test.com")
                .passwordHash("secret")
                .role(role)
                .build();
        u.setId(id);
        return u;
    }

    private Lease lease(Long landlordId, Long tenantId) {
        Lease l = Lease.builder()
                .landlord(user(landlordId, UserRole.USER))
                .primaryTenant(user(tenantId, UserRole.USER))
                .status(LeaseStatus.ACTIVE)
                .leaseType(LeaseType.FIXED_TERM)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(BigDecimal.valueOf(1000))
                .build();
        l.setId(LEASE_ID);
        return l;
    }

    private Lease leaseWithProperty(Long propertyId) {
        Property property = Property.builder().build();
        property.setId(propertyId);
        Lease l = lease(LANDLORD_ID, TENANT_ID);
        l.setProperty(property);
        return l;
    }
}
