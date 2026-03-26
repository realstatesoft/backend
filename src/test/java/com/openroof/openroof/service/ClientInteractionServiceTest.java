package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.ClientInteractionResponse;
import com.openroof.openroof.dto.agent.CreateClientInteractionRequest;
import com.openroof.openroof.dto.agent.UpdateClientInteractionRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.ClientInteraction;
import com.openroof.openroof.model.enums.InteractionSource;
import com.openroof.openroof.model.enums.InteractionType;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.ClientInteractionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientInteractionServiceTest {

    @Mock
    private ClientInteractionRepository clientInteractionRepository;

    @Mock
    private AgentClientRepository agentClientRepository;

    @InjectMocks
    private ClientInteractionService clientInteractionService;

    @Test
    @DisplayName("create() guarda interacción manual y recalcula métricas")
    void create_savesManualInteractionAndRecalculatesMetrics() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 23, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 23, 10, 31);

        when(agentClientRepository.findById(1L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.save(any(ClientInteraction.class))).thenAnswer(invocation -> {
            ClientInteraction interaction = invocation.getArgument(0);
            interaction.setId(55L);
            interaction.setUpdatedAt(updatedAt);
            return interaction;
        });
        when(clientInteractionRepository.calculateMetricsByAgentClientId(1L))
                .thenReturn(metrics(3L, occurredAt));
        when(agentClientRepository.updateMetricsById(1L, 3, occurredAt)).thenReturn(1);

        ClientInteractionResponse response = clientInteractionService.create(
                1L,
                new CreateClientInteractionRequest(
                        InteractionType.NOTE,
                        "Seguimiento",
                        "Cliente prefiere zona norte",
                        "INFO_CAPTURED",
                        occurredAt));

        ArgumentCaptor<ClientInteraction> captor = ArgumentCaptor.forClass(ClientInteraction.class);
        verify(clientInteractionRepository).save(captor.capture());
        verify(agentClientRepository).updateMetricsById(1L, 3, occurredAt);

        ClientInteraction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(InteractionType.NOTE);
        assertThat(saved.getSource()).isEqualTo(InteractionSource.MANUAL);
        assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.agentId()).isEqualTo(7L);
        assertThat(response.type()).isEqualTo("NOTE");

    }

    @Test
    @DisplayName("create() usa fecha actual cuando occurredAt es null")
    void create_usesNowWhenOccurredAtIsNull() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        when(agentClientRepository.findById(1L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.save(any(ClientInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientInteractionRepository.calculateMetricsByAgentClientId(1L))
                .thenReturn(metrics(1L, LocalDateTime.now()));
        when(agentClientRepository.updateMetricsById(eq(1L), eq(1), any(LocalDateTime.class))).thenReturn(1);

        clientInteractionService.create(
                1L,
                new CreateClientInteractionRequest(
                        InteractionType.CALL,
                        "Primera llamada",
                        "Contacto inicial",
                        "CONTACTED",
                        null));

        ArgumentCaptor<ClientInteraction> captor = ArgumentCaptor.forClass(ClientInteraction.class);
        verify(clientInteractionRepository).save(captor.capture());
        assertThat(captor.getValue().getOccurredAt()).isAfter(before);
    }

    @Test
    @DisplayName("create() falla cuando el AgentClient no existe")
    void create_throwsWhenAgentClientDoesNotExist() {
        when(agentClientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientInteractionService.create(
                999L,
                new CreateClientInteractionRequest(
                        InteractionType.EMAIL,
                        "Asunto",
                        "Nota",
                        "SENT",
                        LocalDateTime.of(2026, 3, 20, 8, 0))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con ID: 999");
    }

    @Test
    @DisplayName("update() actualiza campos y recalcula métricas")
    void update_updatesFieldsAndRecalculatesMetrics() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        ClientInteraction interaction = interaction(77L, agentClient, InteractionType.EMAIL);
        LocalDateTime newOccurredAt = LocalDateTime.of(2026, 3, 24, 15, 45);

        when(agentClientRepository.findById(1L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.findByIdAndAgentClient_IdAndDeletedAtIsNull(77L, 1L))
                .thenReturn(Optional.of(interaction));
        when(clientInteractionRepository.save(interaction)).thenReturn(interaction);
        when(clientInteractionRepository.calculateMetricsByAgentClientId(1L))
                .thenReturn(metrics(4L, newOccurredAt));
        when(agentClientRepository.updateMetricsById(1L, 4, newOccurredAt)).thenReturn(1);

        ClientInteractionResponse response = clientInteractionService.update(
                1L,
                77L,
                new UpdateClientInteractionRequest(
                        "Asunto actualizado",
                        "Nota actualizada",
                        "QUALIFIED",
                        newOccurredAt));

        assertThat(interaction.getSubject()).isEqualTo("Asunto actualizado");
        assertThat(interaction.getNote()).isEqualTo("Nota actualizada");
        assertThat(interaction.getOutcome()).isEqualTo("QUALIFIED");
        assertThat(interaction.getOccurredAt()).isEqualTo(newOccurredAt);

        assertThat(response.outcome()).isEqualTo("QUALIFIED");
        verify(agentClientRepository).updateMetricsById(1L, 4, newOccurredAt);
    }

    @Test
    @DisplayName("delete() hace soft-delete y recalcula métricas")
    void delete_softDeletesAndRecalculatesMetrics() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        ClientInteraction interaction = interaction(88L, agentClient, InteractionType.WHATSAPP);
        LocalDateTime remainingLastContact = LocalDateTime.of(2026, 3, 19, 18, 0);

        when(agentClientRepository.findById(1L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.findByIdAndAgentClient_Id(88L, 1L)).thenReturn(Optional.of(interaction));
        when(clientInteractionRepository.save(interaction)).thenReturn(interaction);
        when(clientInteractionRepository.calculateMetricsByAgentClientId(1L))
                .thenReturn(metrics(2L, remainingLastContact));
        when(agentClientRepository.updateMetricsById(1L, 2, remainingLastContact)).thenReturn(1);

        clientInteractionService.delete(1L, 88L);

        assertThat(interaction.getDeletedAt()).isNotNull();

        verify(agentClientRepository).updateMetricsById(1L, 2, remainingLastContact);

    }

    @Test
    @DisplayName("delete() no hace nada cuando la interacción ya estaba borrada")
    void delete_doesNothingWhenInteractionAlreadyDeleted() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        ClientInteraction interaction = interaction(88L, agentClient, InteractionType.WHATSAPP);
        interaction.setDeletedAt(LocalDateTime.of(2026, 3, 20, 9, 0));

        when(agentClientRepository.findById(1L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.findByIdAndAgentClient_Id(88L, 1L)).thenReturn(Optional.of(interaction));

        clientInteractionService.delete(1L, 88L);

        verify(clientInteractionRepository, never()).save(interaction);
        verify(clientInteractionRepository, never()).calculateMetricsByAgentClientId(any());
        verify(agentClientRepository, never()).updateMetricsById(any(), any(), any());
    }

    @Test
    @DisplayName("recordVisitConfirmed() devuelve true y crea interacción SYSTEM cuando existe vínculo")
    void recordVisitConfirmed_returnsTrueAndCreatesSystemInteraction() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        LocalDateTime lastContact = LocalDateTime.of(2026, 3, 25, 15, 0);

        when(agentClientRepository.findByAgent_IdAndUser_Id(7L, 20L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.save(any(ClientInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientInteractionRepository.calculateMetricsByAgentClientId(1L))
                .thenReturn(metrics(1L, lastContact));
        when(agentClientRepository.updateMetricsById(1L, 1, lastContact)).thenReturn(1);

        boolean recorded = clientInteractionService.recordVisitConfirmed(
                7L,
                20L,
                88L,
                LocalDateTime.of(2026, 3, 25, 15, 0));

        ArgumentCaptor<ClientInteraction> captor = ArgumentCaptor.forClass(ClientInteraction.class);
        verify(clientInteractionRepository).save(captor.capture());
        verify(agentClientRepository).updateMetricsById(1L, 1, lastContact);

        ClientInteraction saved = captor.getValue();
        assertThat(recorded).isTrue();
        assertThat(saved.getType()).isEqualTo(InteractionType.VISIT);
        assertThat(saved.getSource()).isEqualTo(InteractionSource.SYSTEM);
        assertThat(saved.getOutcome()).isEqualTo("CONFIRMED");

    }

    @Test
    @DisplayName("recordVisitConfirmed() devuelve false si no existe vínculo agente-cliente")
    void recordVisitConfirmed_returnsFalseWhenAgentClientIsMissing() {
        when(agentClientRepository.findByAgent_IdAndUser_Id(7L, 20L)).thenReturn(Optional.empty());

        boolean recorded = clientInteractionService.recordVisitConfirmed(
                7L,
                20L,
                88L,
                LocalDateTime.of(2026, 3, 25, 15, 0));

        assertThat(recorded).isFalse();
        verify(clientInteractionRepository, never()).save(any(ClientInteraction.class));
        verify(agentClientRepository, never()).updateMetricsById(any(), any(), any());
    }

    @Test
    @DisplayName("recordMessageSent() crea interacción SYSTEM y recalcula métricas cuando existe vínculo")
    void recordMessageSent_createsSystemInteractionAndRecalculatesMetrics() {
        AgentClient agentClient = agentClient(1L, 7L, 20L);
        LocalDateTime lastContact = LocalDateTime.of(2026, 3, 26, 9, 15);

        when(agentClientRepository.findByAgent_IdAndUser_Id(7L, 20L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.save(any(ClientInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientInteractionRepository.calculateMetricsByAgentClientId(1L))
                .thenReturn(metrics(5L, lastContact));
        when(agentClientRepository.updateMetricsById(1L, 5, lastContact)).thenReturn(1);

        clientInteractionService.recordMessageSent(
                7L,
                20L,
                InteractionType.WHATSAPP,
                "Recordatorio",
                "Se envio mensaje de seguimiento");

        ArgumentCaptor<ClientInteraction> captor = ArgumentCaptor.forClass(ClientInteraction.class);
        verify(clientInteractionRepository).save(captor.capture());
        verify(agentClientRepository).updateMetricsById(1L, 5, lastContact);

        ClientInteraction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(InteractionType.WHATSAPP);
        assertThat(saved.getSource()).isEqualTo(InteractionSource.SYSTEM);
        assertThat(saved.getOutcome()).isEqualTo("SENT");

    }

    private AgentClient agentClient(Long id, Long agentId, Long userId) {
        AgentProfile agent = new AgentProfile();
        agent.setId(agentId);

        User user = new User();
        user.setId(userId);

        AgentClient agentClient = new AgentClient();
        agentClient.setId(id);
        agentClient.setAgent(agent);
        agentClient.setUser(user);
        return agentClient;
    }

    private ClientInteraction interaction(Long id, AgentClient agentClient, InteractionType type) {
        ClientInteraction interaction = new ClientInteraction();
        interaction.setId(id);
        interaction.setAgent(agentClient.getAgent());
        interaction.setAgentClient(agentClient);
        interaction.setType(type);
        interaction.setOccurredAt(LocalDateTime.of(2026, 3, 20, 11, 0));
        return interaction;
    }

    private ClientInteractionRepository.AgentClientInteractionMetrics metrics(long count, LocalDateTime lastContactAt) {
        return new ClientInteractionRepository.AgentClientInteractionMetrics() {
            @Override
            public long getInteractionsCount() {
                return count;
            }

            @Override
            public LocalDateTime getLastContactAt() {
                return lastContactAt;
            }
        };
    }
}
