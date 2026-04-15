package com.openroof.openroof.service;

import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.UserPreferenceMapper;
import com.openroof.openroof.model.preference.PreferenceCategory;
import com.openroof.openroof.model.preference.PreferenceOption;
import com.openroof.openroof.model.preference.UserPreference;
import com.openroof.openroof.model.preference.UserPreferenceRange;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PreferenceCategoryRepository;
import com.openroof.openroof.repository.PreferenceOptionRepository;
import com.openroof.openroof.repository.UserPreferenceRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private User user(Long id) {
        User u = User.builder().email("test@test.com").build();
        u.setId(id);
        u.setOnboardingCompleted(false);
        return u;
    }

    // ─── GET OPTIONS ─────────────────────────────────────────────────────────

    @Test
    void getPreferenceOptions_returnsMappedCategories() {
        PreferenceCategory cat1 = PreferenceCategory.builder().id(1L).code("ZONE").name("Zona").build();
        PreferenceOption opt1 = PreferenceOption.builder().id(10L).category(cat1).label("Asunción").value("ASUNCION").displayOrder(1).build();
        cat1.setOptions(List.of(opt1));

        when(preferenceCategoryRepository.findAllWithOptions()).thenReturn(List.of(cat1));

        List<PreferenceCategoryDTO> res = userPreferenceService.getPreferenceOptions();

        assertEquals(1, res.size());
        assertEquals("ZONE", res.get(0).code());
        assertEquals(1, res.get(0).options().size());
        assertEquals("Asunción", res.get(0).options().get(0).label());
    }

    // ─── GET USER PREFERENCES ────────────────────────────────────────────────

    @Test
    void getUserPreferences_userDoesNotExist_throwsException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userPreferenceService.getUserPreferences(1L));
    }

    @Test
    void getUserPreferences_noPreferences_returnsEmptyDTO() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        UserPreferenceResponseDTO res = userPreferenceService.getUserPreferences(1L);

        assertEquals(1L, res.userId());
        assertFalse(res.onboardingCompleted());
        assertTrue(res.selectedOptions().isEmpty());
        assertTrue(res.ranges().isEmpty());
    }

    @Test
    void getUserPreferences_hasPreferences_returnsMappedPreferences() {
        when(userRepository.existsById(1L)).thenReturn(true);
        UserPreference pref = UserPreference.builder()
                .id(100L)
                .user(user(1L))
                .onboardingCompleted(true)
                .build();
        pref.getRanges().add(UserPreferenceRange.builder().fieldName("PRICE").minValue(100D).maxValue(500D).build());

        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        UserPreferenceResponseDTO res = userPreferenceService.getUserPreferences(1L);

        assertEquals(1L, res.userId());
        assertTrue(res.onboardingCompleted());
        assertEquals(1, res.ranges().size());
        assertEquals("PRICE", res.ranges().get(0).fieldName());
    }

    // ─── SAVE OR UPDATE ──────────────────────────────────────────────────────

    @Test
    void saveOrUpdate_userNotFound_throwsException() {
        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(1L, List.of(10L), null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userPreferenceService.saveOrUpdateUserPreferences(req));
    }

    @Test
    void saveOrUpdate_invalidOptionIds_throwsBadRequest() {
        User u = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(preferenceOptionRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(PreferenceOption.builder().id(10L).build()));

        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(1L, List.of(10L, 11L), null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userPreferenceService.saveOrUpdateUserPreferences(req));
        assertTrue(ex.getMessage().contains("11"));
    }

    @Test
    void saveOrUpdate_validData_upsertsPreferences() {
        User u = user(1L);
        PreferenceOption opt1 = PreferenceOption.builder().id(10L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(preferenceOptionRepository.findAllById(List.of(10L))).thenReturn(List.of(opt1));
        
        UserPreference existingPref = UserPreference.builder().id(100L).user(u).build();
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(existingPref));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        List<RangeDTO> ranges = List.of(new RangeDTO("SURFACE", 50D, null));
        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(1L, List.of(10L), ranges);

        UserPreferenceResponseDTO res = userPreferenceService.saveOrUpdateUserPreferences(req);

        assertEquals(1L, res.userId());
        assertTrue(res.onboardingCompleted());
        assertEquals(1, res.selectedOptions().size());
        assertEquals(10L, res.selectedOptions().get(0).id());
        assertEquals(1, res.ranges().size());
        assertEquals("SURFACE", res.ranges().get(0).fieldName());
        
        verify(userPreferenceRepository).save(any(UserPreference.class));
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isOnboardingCompleted());
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Test
    void delete_userNotFound_throwsException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userPreferenceService.deleteUserPreferences(1L));
    }

    @Test
    void delete_preferencesExist_deletesAndResetsUser() {
        User u = user(1L);
        u.setOnboardingCompleted(true);
        UserPreference pref = UserPreference.builder().id(100L).user(u).build();
        
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        userPreferenceService.deleteUserPreferences(1L);

        verify(userPreferenceRepository).delete(pref);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isOnboardingCompleted());
    }
}
