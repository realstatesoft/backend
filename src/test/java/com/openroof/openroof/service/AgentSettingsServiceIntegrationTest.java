package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.AgentSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAgentSettingsRequest;
import com.openroof.openroof.model.config.AgentSettings;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentSettingsRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para AgentSettingsService.
 * Verifica creación automática, persistencia y actualización de ajustes en BD real (H2).
 */
@SpringBootTest
@Transactional
class AgentSettingsServiceIntegrationTest {

    @Autowired AgentSettingsService agentSettingsService;
    @Autowired AgentSettingsRepository agentSettingsRepository;
    @Autowired UserRepository userRepository;

    private User agent;
    private static final String AGENT_EMAIL = "agent@integration.test";

    @BeforeEach
    void setUp() {
        agent = userRepository.save(User.builder()
                .email(AGENT_EMAIL)
                .name("Agente Integración")
                .passwordHash("hashed")
                .role(UserRole.AGENT)
                .build());
    }

    @Nested
    @DisplayName("getSettings() — primer acceso")
    class FirstAccess {

        @Test
        @DisplayName("Crea una fila con defaults en BD al primer acceso")
        void autoCreatesRowWithDefaults() {
            assertThat(agentSettingsRepository.findByUser(agent)).isEmpty();

            AgentSettingsResponse res = agentSettingsService.getSettings(AGENT_EMAIL);

            assertThat(res.autoAssignLeads()).isTrue();
            assertThat(res.notifyNewLead()).isTrue();
            assertThat(res.notifyVisitRequest()).isTrue();
            assertThat(res.notifyNewOffer()).isTrue();
            assertThat(res.workRadiusKm()).isNull();

            assertThat(agentSettingsRepository.findByUser(agent)).isPresent();
        }

        @Test
        @DisplayName("No duplica la fila en accesos consecutivos")
        void noDuplicateRowOnRepeatedAccess() {
            agentSettingsService.getSettings(AGENT_EMAIL);
            agentSettingsService.getSettings(AGENT_EMAIL);

            long count = agentSettingsRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(agent.getId()))
                    .count();
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateSettings()")
    class UpdateSettings {

        @Test
        @DisplayName("Persiste todos los campos actualizados en BD")
        void persistsAllUpdatedFields() {
            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(false, false, true, false, 75));

            AgentSettings saved = agentSettingsRepository.findByUser(agent).orElseThrow();

            assertThat(saved.isAutoAssignLeads()).isFalse();
            assertThat(saved.isNotifyNewLead()).isFalse();
            assertThat(saved.isNotifyVisitRequest()).isTrue();
            assertThat(saved.isNotifyNewOffer()).isFalse();
            assertThat(saved.getWorkRadiusKm()).isEqualTo(75);
        }

        @Test
        @DisplayName("getSettings() refleja los valores actualizados")
        void getAfterUpdate_returnsNewValues() {
            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(false, true, false, true, 100));

            AgentSettingsResponse res = agentSettingsService.getSettings(AGENT_EMAIL);

            assertThat(res.autoAssignLeads()).isFalse();
            assertThat(res.notifyVisitRequest()).isFalse();
            assertThat(res.workRadiusKm()).isEqualTo(100);
        }

        @Test
        @DisplayName("workRadiusKm nulo se persiste como NULL en BD")
        void updateWorkRadiusToNull_persistsNull() {
            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(true, true, true, true, 50));
            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(true, true, true, true, null));

            AgentSettings saved = agentSettingsRepository.findByUser(agent).orElseThrow();
            assertThat(saved.getWorkRadiusKm()).isNull();
        }

        @Test
        @DisplayName("Actualización consecutiva — mantiene el último valor guardado")
        void consecutiveUpdates_lastValueWins() {
            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(true, true, true, true, 30));
            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(false, false, false, false, 200));

            AgentSettingsResponse res = agentSettingsService.getSettings(AGENT_EMAIL);

            assertThat(res.autoAssignLeads()).isFalse();
            assertThat(res.workRadiusKm()).isEqualTo(200);
        }

        @Test
        @DisplayName("Dos agentes distintos tienen sus propias filas de ajustes independientes")
        void twoAgentsHaveIndependentSettings() {
            User agent2 = userRepository.save(User.builder()
                    .email("agent2@integration.test")
                    .name("Agente 2")
                    .passwordHash("hashed")
                    .role(UserRole.AGENT)
                    .build());

            agentSettingsService.updateSettings(AGENT_EMAIL,
                    new UpdateAgentSettingsRequest(false, false, false, false, 50));
            agentSettingsService.updateSettings("agent2@integration.test",
                    new UpdateAgentSettingsRequest(true, true, true, true, 200));

            AgentSettings settings1 = agentSettingsRepository.findByUser(agent).orElseThrow();
            AgentSettings settings2 = agentSettingsRepository.findByUser(agent2).orElseThrow();

            assertThat(settings1.isAutoAssignLeads()).isFalse();
            assertThat(settings2.isAutoAssignLeads()).isTrue();
            assertThat(settings1.getWorkRadiusKm()).isEqualTo(50);
            assertThat(settings2.getWorkRadiusKm()).isEqualTo(200);
        }
    }
}
