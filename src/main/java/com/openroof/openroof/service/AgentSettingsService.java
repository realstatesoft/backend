package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.AgentSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAgentSettingsRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.config.AgentSettings;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentSettingsRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentSettingsService {

    private final AgentSettingsRepository repo;
    private final UserRepository userRepository;

    @Transactional
    public AgentSettingsResponse getSettings(String email) {
        return toResponse(getOrCreate(getUser(email)));
    }

    @Transactional
    public AgentSettingsResponse updateSettings(String email, UpdateAgentSettingsRequest req) {
        User user = getUser(email);
        AgentSettings settings = getOrCreate(user);
        settings.setAutoAssignLeads(req.autoAssignLeads());
        settings.setNotifyNewLead(req.notifyNewLead());
        settings.setNotifyVisitRequest(req.notifyVisitRequest());
        settings.setNotifyNewOffer(req.notifyNewOffer());
        settings.setWorkRadiusKm(req.workRadiusKm());
        return toResponse(repo.save(settings));
    }

    private AgentSettings getOrCreate(User user) {
        return repo.findByUser(user).orElseGet(() -> {
            try {
                return repo.save(AgentSettings.builder().user(user).build());
            } catch (DataIntegrityViolationException e) {
                return repo.findByUser(user)
                        .orElseThrow(() -> new ResourceNotFoundException("Configuración de agente no encontrada"));
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

    private AgentSettingsResponse toResponse(AgentSettings s) {
        return new AgentSettingsResponse(
                s.isAutoAssignLeads(),
                s.isNotifyNewLead(),
                s.isNotifyVisitRequest(),
                s.isNotifyNewOffer(),
                s.getWorkRadiusKm()
        );
    }
}
