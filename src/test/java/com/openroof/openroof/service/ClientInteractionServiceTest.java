package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.ClientInteractionResponse;
import com.openroof.openroof.dto.agent.CreateClientInteractionRequest;
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
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("create() incrementa contador y retorna response compacto")
    void create_incrementsCounter() {
        AgentProfile agent = new AgentProfile();
        agent.setId(7L);

        AgentClient agentClient = new AgentClient();
        agentClient.setId(1L);
        agentClient.setAgent(agent);
        agentClient.setInteractionsCount(2);

        CreateClientInteractionRequest request = new CreateClientInteractionRequest(
                InteractionType.NOTE,
                null,
                "Cliente prefiere zona norte",
                "INFO_CAPTURED",
                LocalDateTime.of(2026, 3, 23, 10, 30));

        when(agentClientRepository.findById(1L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.save(any(ClientInteraction.class))).thenAnswer(invocation -> {
            ClientInteraction interaction = invocation.getArgument(0);
            interaction.setId(55L);
            interaction.setUpdatedAt(LocalDateTime.of(2026, 3, 23, 10, 31));
            return interaction;
        });

        ClientInteractionResponse response = clientInteractionService.create(1L, request);

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.agentId()).isEqualTo(7L);
        assertThat(response.type()).isEqualTo("NOTE");
        assertThat(agentClient.getInteractionsCount()).isEqualTo(3);
        verify(agentClientRepository).save(agentClient);
    }

    @Test
    @DisplayName("recordVisitConfirmed() crea interacción SYSTEM cuando existe vínculo agente-cliente")
    void recordVisitConfirmed_createsSystemInteraction() {
        AgentProfile agent = new AgentProfile();
        agent.setId(7L);

        User user = new User();
        user.setId(20L);

        AgentClient agentClient = new AgentClient();
        agentClient.setId(1L);
        agentClient.setAgent(agent);
        agentClient.setUser(user);
        agentClient.setInteractionsCount(0);

        when(agentClientRepository.findByAgent_IdAndUser_Id(7L, 20L)).thenReturn(Optional.of(agentClient));
        when(clientInteractionRepository.save(any(ClientInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clientInteractionService.recordVisitConfirmed(
                7L,
                20L,
                88L,
                LocalDateTime.of(2026, 3, 25, 15, 0));

        ArgumentCaptor<ClientInteraction> captor = ArgumentCaptor.forClass(ClientInteraction.class);
        verify(clientInteractionRepository).save(captor.capture());
        verify(agentClientRepository).save(agentClient);

        ClientInteraction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(InteractionType.VISIT);
        assertThat(saved.getSource()).isEqualTo(InteractionSource.SYSTEM);
        assertThat(saved.getOutcome()).isEqualTo("CONFIRMED");
        assertThat(saved.getAgentClient()).isEqualTo(agentClient);
        assertThat(agentClient.getInteractionsCount()).isEqualTo(1);
    }
}
