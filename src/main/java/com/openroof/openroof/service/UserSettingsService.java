package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.UpdateUserSettingsRequest;
import com.openroof.openroof.dto.settings.UserSettingsResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.config.UserSettings;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingsService {

    private final UserSettingsRepository repo;
    private final UserRepository userRepository;

    @Transactional
    public UserSettingsResponse getSettings(String email) {
        return toResponse(getOrCreate(getUser(email)));
    }

    @Transactional
    public UserSettingsResponse updateSettings(String email, UpdateUserSettingsRequest req) {
        User user = getUser(email);
        UserSettings settings = getOrCreate(user);
        settings.setNotifyPriceDrop(req.notifyPriceDrop());
        settings.setNotifyNewMatch(req.notifyNewMatch());
        settings.setNotifyMessages(req.notifyMessages());
        settings.setNotifyChannel(req.notifyChannel());
        settings.setProfileVisibleToAgents(req.profileVisibleToAgents());
        settings.setAllowDirectContact(req.allowDirectContact());
        return toResponse(repo.save(settings));
    }

    private UserSettings getOrCreate(User user) {
        return repo.findByUser(user).orElseGet(() -> {
            try {
                return repo.save(UserSettings.builder().user(user).build());
            } catch (DataIntegrityViolationException e) {
                return repo.findByUser(user)
                        .orElseThrow(() -> new ResourceNotFoundException("Configuración de usuario no encontrada"));
            }
        });
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("Usuario no encontrado con email: {}", email);
                    return new ResourceNotFoundException("Usuario no encontrado");
                });
    }

    private UserSettingsResponse toResponse(UserSettings s) {
        return new UserSettingsResponse(
                s.isNotifyPriceDrop(),
                s.isNotifyNewMatch(),
                s.isNotifyMessages(),
                s.getNotifyChannel(),
                s.isProfileVisibleToAgents(),
                s.isAllowDirectContact()
        );
    }
}
