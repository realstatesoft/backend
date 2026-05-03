package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.AgentSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAgentSettingsRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.config.AgentSettings;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentSettingsRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentSettingsServiceTest {

    @Mock private AgentSettingsRepository repo;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AgentSettingsService service;

    private User agent;

    @BeforeEach
    void setUp() {
        agent = User.builder().name("Agente").email("agent@test.com").role(UserRole.AGENT).build();
        agent.setId(1L);
    }

    private AgentSettings existingSettings() {
        AgentSettings s = AgentSettings.builder()
                .user(agent)
                .autoAssignLeads(false)
                .notifyNewLead(true)
                .notifyVisitRequest(false)
                .notifyNewOffer(true)
                .workRadiusKm(50)
                .build();
        s.setId(10L);
        return s;
    }

    @Nested
    @DisplayName("getSettings()")
    class GetSettings {

        @Test
        @DisplayName("Retorna los ajustes existentes del agente")
        void returnsExistingSettings() {
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
            when(repo.findByUser(agent)).thenReturn(Optional.of(existingSettings()));

            AgentSettingsResponse res = service.getSettings("agent@test.com");

            assertThat(res.autoAssignLeads()).isFalse();
            assertThat(res.notifyNewLead()).isTrue();
            assertThat(res.notifyVisitRequest()).isFalse();
            assertThat(res.notifyNewOffer()).isTrue();
            assertThat(res.workRadiusKm()).isEqualTo(50);
        }

        @Test
        @DisplayName("Auto-crea ajustes con defaults cuando el agente no tiene registro previo")
        void autoCreatesWithDefaultsOnFirstAccess() {
            AgentSettings defaults = AgentSettings.builder().user(agent).build();
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
            when(repo.findByUser(agent)).thenReturn(Optional.empty());
            when(repo.save(any())).thenReturn(defaults);

            AgentSettingsResponse res = service.getSettings("agent@test.com");

            assertThat(res.autoAssignLeads()).isTrue();
            assertThat(res.notifyNewLead()).isTrue();
            assertThat(res.notifyVisitRequest()).isTrue();
            assertThat(res.notifyNewOffer()).isTrue();
            assertThat(res.workRadiusKm()).isNull();
            verify(repo).save(any(AgentSettings.class));
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el usuario no existe")
        void userNotFound_throwsResourceNotFound() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSettings("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateSettings()")
    class UpdateSettings {

        @Test
        @DisplayName("Actualiza todos los campos y los persiste")
        void updatesAllFields() {
            AgentSettings existing = existingSettings();
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
            when(repo.findByUser(agent)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AgentSettingsResponse res = service.updateSettings("agent@test.com",
                    new UpdateAgentSettingsRequest(true, false, true, false, 100));

            assertThat(res.autoAssignLeads()).isTrue();
            assertThat(res.notifyNewLead()).isFalse();
            assertThat(res.notifyVisitRequest()).isTrue();
            assertThat(res.notifyNewOffer()).isFalse();
            assertThat(res.workRadiusKm()).isEqualTo(100);
        }

        @Test
        @DisplayName("Persiste la entidad modificada a través del repositorio")
        void callsRepoSaveWithMutatedEntity() {
            AgentSettings existing = existingSettings();
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
            when(repo.findByUser(agent)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateSettings("agent@test.com",
                    new UpdateAgentSettingsRequest(true, true, true, true, 200));

            ArgumentCaptor<AgentSettings> captor = ArgumentCaptor.forClass(AgentSettings.class);
            verify(repo).save(captor.capture());
            assertThat(captor.getValue().getWorkRadiusKm()).isEqualTo(200);
            assertThat(captor.getValue().isAutoAssignLeads()).isTrue();
        }

        @Test
        @DisplayName("Acepta workRadiusKm nulo para representar radio ilimitado")
        void acceptsNullWorkRadius() {
            AgentSettings existing = existingSettings();
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
            when(repo.findByUser(agent)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AgentSettingsResponse res = service.updateSettings("agent@test.com",
                    new UpdateAgentSettingsRequest(true, true, true, true, null));

            assertThat(res.workRadiusKm()).isNull();
        }
    }
}
