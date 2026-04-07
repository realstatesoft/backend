package com.openroof.openroof.service;

import com.openroof.openroof.dto.visit.VisitRequestResponse;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.model.interaction.VisitRequest;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.VisitRepository;
import com.openroof.openroof.repository.VisitRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitRequestServiceTest {

    @Mock
    private VisitRequestRepository visitRequestRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AgentProfileRepository agentProfileRepository;

    @Mock
    private AgentClientRepository agentClientRepository;

    @Mock
    private ClientInteractionService clientInteractionService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VisitRequestService visitRequestService;

    @Test
    @DisplayName("accept() crea AgentClient faltante y reintenta registrar interacción")
    void accept_createsMissingAgentClientAndRetriesInteractionRecording() {
        User currentUser = new User();
        currentUser.setId(100L);
        currentUser.setEmail("agent@openroof.com");
        currentUser.setRole(UserRole.AGENT);

        User agentUser = new User();
        agentUser.setId(200L);
        agentUser.setName("Agente Test");

        AgentProfile agent = new AgentProfile();
        agent.setId(10L);
        agent.setUser(agentUser);

        User buyer = new User();
        buyer.setId(20L);
        buyer.setName("Comprador");
        buyer.setEmail("buyer@openroof.com");
        buyer.setPhone("0991-000000");

        Property property = new Property();
        property.setId(30L);
        property.setTitle("Casa moderna");
        property.setAgent(agent);
        property.setOwner(new User());

        LocalDateTime proposedAt = LocalDateTime.of(2026, 3, 28, 16, 0);

        VisitRequest visitRequest = new VisitRequest();
        visitRequest.setId(40L);
        visitRequest.setProperty(property);
        visitRequest.setBuyer(buyer);
        visitRequest.setAgent(agent);
        visitRequest.setProposedAt(proposedAt);
        visitRequest.setStatus(VisitRequestStatus.PENDING);
        visitRequest.setBuyerName(buyer.getName());
        visitRequest.setBuyerEmail(buyer.getEmail());
        visitRequest.setBuyerPhone(buyer.getPhone());
        visitRequest.setMessage("Quiero visitar esta propiedad");

        when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(currentUser));
        when(visitRequestRepository.findById(40L)).thenReturn(Optional.of(visitRequest));
        when(agentProfileRepository.findByUser_Id(100L)).thenReturn(Optional.of(agent));
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
            Visit visit = invocation.getArgument(0);
            visit.setId(99L);
            return visit;
        });
        when(clientInteractionService.recordVisitConfirmed(10L, 20L, 30L, proposedAt))
                .thenReturn(false, true);
        when(agentClientRepository.findByAgent_IdAndUser_Id(10L, 20L)).thenReturn(Optional.empty());
        when(agentClientRepository.save(any(AgentClient.class))).thenAnswer(invocation -> {
            AgentClient agentClient = invocation.getArgument(0);
            agentClient.setId(501L);
            return agentClient;
        });
        when(visitRequestRepository.save(visitRequest)).thenReturn(visitRequest);

        VisitRequestResponse response = visitRequestService.accept(40L, "agent@openroof.com");

        assertThat(response.status()).isEqualTo(VisitRequestStatus.ACCEPTED);
        assertThat(response.visitId()).isEqualTo(99L);
        assertThat(visitRequest.getVisit()).isNotNull();
        verify(agentClientRepository).save(any(AgentClient.class));
        verify(clientInteractionService, times(2))
                .recordVisitConfirmed(10L, 20L, 30L, proposedAt);
    }

    @Test
    @DisplayName("accept() no crea AgentClient cuando la interacción se registra en el primer intento")
    void accept_doesNotCreateAgentClientWhenInteractionIsRecordedImmediately() {
        User currentUser = new User();
        currentUser.setId(100L);
        currentUser.setEmail("agent@openroof.com");
        currentUser.setRole(UserRole.AGENT);

        User agentUser = new User();
        agentUser.setId(200L);
        agentUser.setName("Agente Test");

        AgentProfile agent = new AgentProfile();
        agent.setId(10L);
        agent.setUser(agentUser);

        User buyer = new User();
        buyer.setId(20L);
        buyer.setName("Comprador");
        buyer.setEmail("buyer@openroof.com");
        buyer.setPhone("0991-000000");

        Property property = new Property();
        property.setId(30L);
        property.setTitle("Casa moderna");
        property.setAgent(agent);
        property.setOwner(new User());

        LocalDateTime proposedAt = LocalDateTime.of(2026, 3, 28, 16, 0);

        VisitRequest visitRequest = new VisitRequest();
        visitRequest.setId(40L);
        visitRequest.setProperty(property);
        visitRequest.setBuyer(buyer);
        visitRequest.setAgent(agent);
        visitRequest.setProposedAt(proposedAt);
        visitRequest.setStatus(VisitRequestStatus.PENDING);
        visitRequest.setBuyerName(buyer.getName());
        visitRequest.setBuyerEmail(buyer.getEmail());
        visitRequest.setBuyerPhone(buyer.getPhone());

        when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(currentUser));
        when(visitRequestRepository.findById(40L)).thenReturn(Optional.of(visitRequest));
        when(agentProfileRepository.findByUser_Id(100L)).thenReturn(Optional.of(agent));
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
            Visit visit = invocation.getArgument(0);
            visit.setId(99L);
            return visit;
        });
        when(clientInteractionService.recordVisitConfirmed(10L, 20L, 30L, proposedAt))
                .thenReturn(true);
        when(visitRequestRepository.save(visitRequest)).thenReturn(visitRequest);

        VisitRequestResponse response = visitRequestService.accept(40L, "agent@openroof.com");

        assertThat(response.status()).isEqualTo(VisitRequestStatus.ACCEPTED);
        verify(agentClientRepository, never()).findByAgent_IdAndUser_Id(any(), any());
        verify(agentClientRepository, never()).save(any(AgentClient.class));
        verify(clientInteractionService)
                .recordVisitConfirmed(10L, 20L, 30L, proposedAt);
    }
}
