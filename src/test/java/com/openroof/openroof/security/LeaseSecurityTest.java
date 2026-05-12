package com.openroof.openroof.security;

import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseSecurityTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LANDLORD_ID = 2L;
    private static final Long TENANT_ID = 3L;
    private static final Long OTHER_ID = 4L;
    private static final Long LEASE_ID = 100L;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private UserRepository userRepository;

    private LeaseSecurity leaseSecurity;

    @BeforeEach
    void setUp() {
        leaseSecurity = new LeaseSecurity(leaseRepository, userRepository);
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
    void hasLeaseAccess_nullLeaseId_returnsFalse() {
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
}
