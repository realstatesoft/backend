package com.openroof.openroof.service;

import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.mapper.UserPreferenceMapper;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.preference.PreferenceCategory;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PreferenceCategoryRepository;
import com.openroof.openroof.repository.PreferenceOptionRepository;
import com.openroof.openroof.repository.UserPreferenceRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;
    @Mock
    private PreferenceCategoryRepository preferenceCategoryRepository;
    @Mock
    private PreferenceOptionRepository preferenceOptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private UserPreferenceMapper userPreferenceMapper;
    private UserPreferenceService userPreferenceService;

    @BeforeEach
    void setUp() {
        userPreferenceMapper = new UserPreferenceMapper();
        userPreferenceService = new UserPreferenceService(
                userPreferenceRepository,
                preferenceCategoryRepository,
                preferenceOptionRepository,
                userRepository,
                userPreferenceMapper
        );
    }

    private void mockAuth(Long userId, UserRole role) {
        User u = user(userId);
        u.setRole(role);
        when(authentication.getPrincipal()).thenReturn(u);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private User user(Long id) {
        User u = User.builder().email("test@test.com").role(UserRole.USER).build();
        u.setId(id);
        u.setOnboardingCompleted(false);
        return u;
    }

    // ─── GET OPTIONS ─────────────────────────────────────────────────────────

    @Test
    void getPreferenceOptions_returnsMappedCategories() {
        PreferenceCategory cat1 = PreferenceCategory.builder().id(1L).code("ZONE").name("Zona").build();
        PreferenceOption opt1 = PreferenceOption.builder().id(10L).category(cat1).label("Asunción").value("ASUNCION").displayOrder(1).build();
        cat1.setOptions(new ArrayList<>(List.of(opt1)));

        when(preferenceCategoryRepository.findAllWithOptions()).thenReturn(List.of(cat1));

        List<PreferenceCategoryDTO> res = userPreferenceService.getPreferenceOptions();

        assertEquals(1, res.size());
        assertEquals("ZONE", res.get(0).code());
    }

    // ─── GET USER PREFERENCES ────────────────────────────────────────────────

    @Test
    void getUserPreferences_otherUser_throwsForbidden() {
        mockAuth(1L, UserRole.USER); // I am user 1
        assertThrows(ForbiddenException.class, () -> userPreferenceService.getUserPreferences(2L));
    }

    @Test
    void getUserPreferences_ownPreferences_returnsOk() {
        mockAuth(1L, UserRole.USER);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        UserPreferenceResponseDTO res = userPreferenceService.getUserPreferences(1L);
        assertEquals(1L, res.userId());
    }

    @Test
    void getUserPreferences_otherUserAsAdmin_returnsOk() {
        mockAuth(99L, UserRole.ADMIN);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        UserPreferenceResponseDTO res = userPreferenceService.getUserPreferences(1L);
        assertEquals(1L, res.userId());
    }

    // ─── SAVE OR UPDATE ──────────────────────────────────────────────────────

    @Test
    void saveOrUpdate_validData_upsertsPreferences() {
        mockAuth(1L, UserRole.USER);
        User u = user(1L);
        PreferenceOption opt1 = PreferenceOption.builder().id(10L).build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(preferenceOptionRepository.findAllById(List.of(10L))).thenReturn(List.of(opt1));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(1L, List.of(10L), List.of());
        UserPreferenceResponseDTO res = userPreferenceService.saveOrUpdateUserPreferences(req);

        assertEquals(1L, res.userId());
        assertTrue(res.onboardingCompleted());
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    void saveOrUpdate_invalidOptionIds_throwsBadRequest() {
        mockAuth(1L, UserRole.USER);
        User u = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(preferenceOptionRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(PreferenceOption.builder().id(10L).build()));

        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(1L, List.of(10L, 11L), null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userPreferenceService.saveOrUpdateUserPreferences(req));
        assertTrue(ex.getMessage().contains("11"));
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Test
    void delete_preferencesExist_deletesAndResetsUser() {
        mockAuth(1L, UserRole.USER);
        User u = user(1L);
        UserPreference pref = UserPreference.builder().id(100L).user(u).build();
        
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        userPreferenceService.deleteUserPreferences(1L);

        verify(userPreferenceRepository).delete(pref);
        verify(userRepository).save(any(User.class));
    }
}
