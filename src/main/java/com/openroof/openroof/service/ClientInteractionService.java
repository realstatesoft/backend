package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.ClientInteractionResponse;
import com.openroof.openroof.dto.agent.CreateClientInteractionRequest;
import com.openroof.openroof.dto.agent.UpdateClientInteractionRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.ClientInteraction;
import com.openroof.openroof.model.enums.InteractionSource;
import com.openroof.openroof.model.enums.InteractionType;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.ClientInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientInteractionService {

    private final ClientInteractionRepository clientInteractionRepository;
    private final AgentClientRepository agentClientRepository;

    public ClientInteractionResponse create(Long agentClientId, CreateClientInteractionRequest request) {
        AgentClient agentClient = findAgentClient(agentClientId);

        ClientInteraction interaction = ClientInteraction.builder()
                .agent(agentClient.getAgent())
                .agentClient(agentClient)
                .type(request.type())
                .subject(request.subject())
                .note(request.note())
                .outcome(request.outcome())
                .source(InteractionSource.MANUAL)
                .occurredAt(request.occurredAt() != null ? request.occurredAt() : LocalDateTime.now())
                .build();

        ClientInteraction saved = clientInteractionRepository.save(interaction);
        recalcInteractionMetricsForAgentClient(agentClient.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ClientInteractionResponse> list(Long agentClientId, InteractionType type, Pageable pageable) {
        findAgentClient(agentClientId);

        Page<ClientInteraction> page = type != null
                ? clientInteractionRepository.findByAgentClient_IdAndTypeAndDeletedAtIsNull(agentClientId, type, pageable)
                : clientInteractionRepository.findByAgentClient_IdAndDeletedAtIsNull(agentClientId, pageable);

        return page.map(this::toResponse);
    }

    public ClientInteractionResponse update(Long agentClientId, Long interactionId, UpdateClientInteractionRequest request) {
        ClientInteraction interaction = findActiveInteraction(agentClientId, interactionId);

        if (request.subject() != null) {
            interaction.setSubject(request.subject());
        }
        if (request.note() != null) {
            interaction.setNote(request.note());
        }
        if (request.outcome() != null) {
            interaction.setOutcome(request.outcome());
        }
        if (request.occurredAt() != null) {
            interaction.setOccurredAt(request.occurredAt());
        }

        ClientInteraction saved = clientInteractionRepository.save(interaction);
        recalcInteractionMetricsForAgentClient(agentClientId);
        return toResponse(saved);
    }

    public void delete(Long agentClientId, Long interactionId) {
        ClientInteraction interaction = findInteraction(agentClientId, interactionId);
        if (interaction.getDeletedAt() == null) {
            interaction.softDelete();
            clientInteractionRepository.save(interaction);
            recalcInteractionMetricsForAgentClient(agentClientId);
        }
    }

    public boolean recordVisitConfirmed(Long agentId, Long userId, Long propertyId, LocalDateTime scheduledAt) {
        return agentClientRepository.findByAgent_IdAndUser_Id(agentId, userId)
                .map(agentClient -> {
                    recordSystemInteraction(
                            agentClient,
                            InteractionType.VISIT,
                            "Visita confirmada",
                            "Se confirmo la visita para la propiedad " + propertyId + " el " + scheduledAt,
                            "CONFIRMED",
                            LocalDateTime.now()
                    );
                    return true;
                })
                .orElse(false);
    }

    public void recordMessageSent(Long agentId, Long userId, InteractionType type, String subject, String note) {
        agentClientRepository.findByAgent_IdAndUser_Id(agentId, userId)
                .ifPresent(agentClient -> recordSystemInteraction(
                        agentClient,
                        type,
                        subject,
                        note,
                        "SENT",
                        LocalDateTime.now()
                ));
    }

    private void recordSystemInteraction(
            AgentClient agentClient,
            InteractionType type,
            String subject,
            String note,
            String outcome,
            LocalDateTime occurredAt
    ) {
        ClientInteraction interaction = ClientInteraction.builder()
                .agent(agentClient.getAgent())
                .agentClient(agentClient)
                .type(type)
                .subject(subject)
                .note(note)
                .outcome(outcome)
                .source(InteractionSource.SYSTEM)
                .occurredAt(occurredAt)
                .build();

        clientInteractionRepository.save(interaction);
        recalcInteractionMetricsForAgentClient(agentClient.getId());
    }

    private AgentClient findAgentClient(Long agentClientId) {
        return agentClientRepository.findById(agentClientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con ID: " + agentClientId));
    }

    private ClientInteraction findInteraction(Long agentClientId, Long interactionId) {
        return findInteraction(agentClientId, interactionId, false);
    }

    private ClientInteraction findActiveInteraction(Long agentClientId, Long interactionId) {
        return findInteraction(agentClientId, interactionId, true);
    }

    private ClientInteraction findInteraction(Long agentClientId, Long interactionId, boolean activeOnly) {
        findAgentClient(agentClientId);
        return (activeOnly
                ? clientInteractionRepository.findByIdAndAgentClient_IdAndDeletedAtIsNull(interactionId, agentClientId)
                : clientInteractionRepository.findByIdAndAgentClient_Id(interactionId, agentClientId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interaccion no encontrada con ID: " + interactionId));
    }

    private void bumpInteractionCount(AgentClient agentClient, int delta) {
        recalcInteractionMetricsForAgentClient(agentClient.getId());
    }

    private void recalcInteractionMetricsForAgentClient(Long agentClientId) {
        ClientInteractionRepository.AgentClientInteractionMetrics metrics =
                clientInteractionRepository.calculateMetricsByAgentClientId(agentClientId);

        int interactionsCount = metrics != null ? Math.toIntExact(metrics.getInteractionsCount()) : 0;

        LocalDateTime lastContactAt = metrics != null ? metrics.getLastContactAt() : null;
        agentClientRepository.updateMetricsById(agentClientId, interactionsCount, lastContactAt);

    }

    private ClientInteractionResponse toResponse(ClientInteraction interaction) {
        return new ClientInteractionResponse(
                interaction.getId(),
                interaction.getAgent() != null ? interaction.getAgent().getId() : null,
                interaction.getType().name(),
                interaction.getSubject(),
                interaction.getNote(),
                interaction.getOutcome(),
                interaction.getSource().name(),
                interaction.getOccurredAt(),
                interaction.getUpdatedAt()
        );
    }
}
