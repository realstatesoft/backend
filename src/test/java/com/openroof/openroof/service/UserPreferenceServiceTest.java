package com.openroof.openroof.service;

import com.openroof.openroof.dto.preference.UserPreferenceRequestDTO;
import com.openroof.openroof.dto.preference.UserPreferenceResponseDTO;
import com.openroof.openroof.mapper.UserPreferenceMapper;
import com.openroof.openroof.model.enums.UserRole;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private PreferenceCategoryRepository preferenceCategoryRepository;
    @Mock private PreferenceOptionRepository preferenceOptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserPreferenceMapper userPreferenceMapper;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    private UserPreferenceService userPreferenceService;

    @BeforeEach
    void setUp() {
        userPreferenceService = new UserPreferenceService(
                userPreferenceRepository,
                preferenceCategoryRepository,
                preferenceOptionRepository,
                userRepository,
                userPreferenceMapper
        );
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getUserPreferences_whenExists_returnsMappedDTO() {
        Long userId = 1L;
        User user = User.builder().role(UserRole.USER).build();
        user.setId(userId);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        when(userRepository.existsById(userId)).thenReturn(true);
        
        UserPreference pref = new UserPreference();
        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(userPreferenceMapper.toResponseDTO(pref)).thenReturn(new UserPreferenceResponseDTO(userId, true, Collections.emptyList(), Collections.emptyList()));

        UserPreferenceResponseDTO result = userPreferenceService.getUserPreferences(userId);
        
        assertNotNull(result);
        assertTrue(result.onboardingCompleted());
    }

    @Test
    void saveOrUpdate_callsBothRepositories() {
        Long userId = 1L;
        User user = User.builder().role(UserRole.USER).build();
        user.setId(userId);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        UserPreferenceRequestDTO request = new UserPreferenceRequestDTO(userId, List.of(10L), Collections.emptyList());
        PreferenceOption option = new PreferenceOption();
        when(preferenceOptionRepository.findAllById(any())).thenReturn(List.of(option));
        
        UserPreference savedPref = new UserPreference();
        when(userPreferenceRepository.save(any())).thenReturn(savedPref);
        
        userPreferenceService.saveOrUpdateUserPreferences(request);
        
        verify(userPreferenceRepository).save(any());
        assertTrue(user.isOnboardingCompleted());
        verify(userRepository).save(user);
    }
}
