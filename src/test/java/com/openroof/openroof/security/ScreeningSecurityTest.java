package com.openroof.openroof.security;

import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.TenantScreeningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningSecurityTest {

    private static final Long APP_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long AGENT_USER_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;
    private static final Long ADMIN_ID = 4L;

    @Mock
    private TenantScreeningRepository screeningRepository;

    @Mock
    private RentalApplicationRepository rentalApplicationRepository;

    private ScreeningSecurity screeningSecurity;

    @BeforeEach
    void setUp() {
        screeningSecurity = new ScreeningSecurity(screeningRepository, rentalApplicationRepository);
    }

    // ─── canManageByApplication ───────────────────────────────────────────────

    @Test
    void canManageByApplication_admin_returnsTrue_withoutTouchingRepo() {
        assertTrue(screeningSecurity.canManageByApplication(APP_ID, user(ADMIN_ID, UserRole.ADMIN)));
        verifyNoInteractions(rentalApplicationRepository);
    }

    @Test
    void canManageByApplication_owner_returnsTrue() {
        when(rentalApplicationRepository.findById(APP_ID))
                .thenReturn(Optional.of(application(OWNER_ID, AGENT_USER_ID)));

        assertTrue(screeningSecurity.canManageByApplication(APP_ID, user(OWNER_ID, UserRole.USER)));
    }

    @Test
    void canManageByApplication_assignedAgent_returnsTrue() {
        when(rentalApplicationRepository.findById(APP_ID))
                .thenReturn(Optional.of(application(OWNER_ID, AGENT_USER_ID)));

        assertTrue(screeningSecurity.canManageByApplication(APP_ID, user(AGENT_USER_ID, UserRole.AGENT)));
    }

    @Test
    void canManageByApplication_outsider_returnsFalse() {
        when(rentalApplicationRepository.findById(APP_ID))
                .thenReturn(Optional.of(application(OWNER_ID, AGENT_USER_ID)));

        assertFalse(screeningSecurity.canManageByApplication(APP_ID, user(OUTSIDER_ID, UserRole.USER)));
    }

    @Test
    void canManageByApplication_applicationNotFound_returnsFalse() {
        when(rentalApplicationRepository.findById(APP_ID)).thenReturn(Optional.empty());

        assertFalse(screeningSecurity.canManageByApplication(APP_ID, user(OWNER_ID, UserRole.USER)));
    }

    @Test
    void canManageByApplication_nullUser_returnsFalse() {
        assertFalse(screeningSecurity.canManageByApplication(APP_ID, null));
        verifyNoInteractions(rentalApplicationRepository);
    }

    // ─── hasReadAccessByApplication ───────────────────────────────────────────

    @Test
    void hasReadAccessByApplication_admin_returnsTrue_withoutTouchingRepo() {
        assertTrue(screeningSecurity.hasReadAccessByApplication(APP_ID, user(ADMIN_ID, UserRole.ADMIN)));
        verifyNoInteractions(rentalApplicationRepository);
    }

    @Test
    void hasReadAccessByApplication_owner_returnsTrue() {
        when(rentalApplicationRepository.findById(APP_ID))
                .thenReturn(Optional.of(application(OWNER_ID, AGENT_USER_ID)));

        assertTrue(screeningSecurity.hasReadAccessByApplication(APP_ID, user(OWNER_ID, UserRole.USER)));
    }

    @Test
    void hasReadAccessByApplication_assignedAgent_returnsTrue() {
        when(rentalApplicationRepository.findById(APP_ID))
                .thenReturn(Optional.of(application(OWNER_ID, AGENT_USER_ID)));

        assertTrue(screeningSecurity.hasReadAccessByApplication(APP_ID, user(AGENT_USER_ID, UserRole.AGENT)));
    }

    @Test
    void hasReadAccessByApplication_outsider_returnsFalse() {
        when(rentalApplicationRepository.findById(APP_ID))
                .thenReturn(Optional.of(application(OWNER_ID, AGENT_USER_ID)));

        assertFalse(screeningSecurity.hasReadAccessByApplication(APP_ID, user(OUTSIDER_ID, UserRole.USER)));
    }

    @Test
    void hasReadAccessByApplication_applicationNotFound_returnsFalse() {
        when(rentalApplicationRepository.findById(APP_ID)).thenReturn(Optional.empty());

        assertFalse(screeningSecurity.hasReadAccessByApplication(APP_ID, user(OWNER_ID, UserRole.USER)));
    }

    @Test
    void hasReadAccessByApplication_nullUser_returnsFalse() {
        assertFalse(screeningSecurity.hasReadAccessByApplication(APP_ID, null));
        verifyNoInteractions(rentalApplicationRepository);
    }

    // ─── hasReadAccess (legacy, por screeningId) ──────────────────────────────

    @Test
    void hasReadAccess_admin_returnsTrue_withoutTouchingRepo() {
        assertTrue(screeningSecurity.hasReadAccess(50L, user(ADMIN_ID, UserRole.ADMIN)));
        verifyNoInteractions(screeningRepository);
        verifyNoInteractions(rentalApplicationRepository);
    }

    @Test
    void hasReadAccess_assignedAgent_returnsTrue() {
        Long screeningId = 50L;
        com.openroof.openroof.model.screening.TenantScreening s =
                com.openroof.openroof.model.screening.TenantScreening.builder()
                        .application(application(OWNER_ID, AGENT_USER_ID))
                        .build();
        s.setId(screeningId);
        when(screeningRepository.findById(screeningId)).thenReturn(Optional.of(s));

        assertTrue(screeningSecurity.hasReadAccess(screeningId, user(AGENT_USER_ID, UserRole.AGENT)));
    }

    @Test
    void hasReadAccess_ownerWithRoleUser_returnsFalse() {
        // Legacy hasReadAccess solo admite ADMIN o AGENT. Owner USER no pasa.
        assertFalse(screeningSecurity.hasReadAccess(50L, user(OWNER_ID, UserRole.USER)));
        verifyNoInteractions(screeningRepository);
    }

    @Test
    void hasReadAccess_screeningNotFound_returnsFalse() {
        Long screeningId = 999L;
        when(screeningRepository.findById(screeningId)).thenReturn(Optional.empty());

        assertFalse(screeningSecurity.hasReadAccess(screeningId, user(AGENT_USER_ID, UserRole.AGENT)));
    }

    @Test
    void hasReadAccess_nullUser_returnsFalse() {
        assertFalse(screeningSecurity.hasReadAccess(50L, null));
        verifyNoInteractions(screeningRepository);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private User user(Long id, UserRole role) {
        User u = User.builder()
                .email(role.name().toLowerCase() + id + "@test.com")
                .passwordHash("secret")
                .role(role)
                .build();
        u.setId(id);
        return u;
    }

    private RentalApplication application(Long ownerId, Long agentUserId) {
        User owner = user(ownerId, UserRole.USER);
        User agentUser = user(agentUserId, UserRole.AGENT);
        AgentProfile agent = AgentProfile.builder().user(agentUser).build();
        Property property = Property.builder().owner(owner).agent(agent).build();
        RentalApplication app = RentalApplication.builder().property(property).build();
        app.setId(APP_ID);
        return app;
    }
}
