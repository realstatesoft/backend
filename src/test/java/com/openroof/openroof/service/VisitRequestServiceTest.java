package com.openroof.openroof.service;

import com.openroof.openroof.dto.visit.CounterProposeRequest;
import com.openroof.openroof.dto.visit.CreateVisitRequestRequest;
import com.openroof.openroof.dto.visit.VisitRequestResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.enums.VisitStatus;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.model.interaction.VisitRequest;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyAssignment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentClientRepository;
import com.openroof.openroof.repository.AgentAgendaRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.PropertyAssignmentRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.VisitRepository;
import com.openroof.openroof.repository.VisitRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private PropertyAssignmentRepository propertyAssignmentRepository;
    @Mock
    private AgentClientRepository agentClientRepository;
    @Mock
    private ClientInteractionService clientInteractionService;

    @Mock
    private AgentAgendaRepository agentAgendaRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VisitRequestService visitRequestService;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 4, 7, 10, 0);
    private User buyerUser;
    private User ownerUser;
    private User assignedAgentUser;
    private AgentProfile assignedAgentProfile;
    private Property property;

    @BeforeEach
    void setUp() {
        buyerUser = createUser(101L, "Buyer", "buyer@openroof.com", UserRole.USER);
        ownerUser = createUser(201L, "Owner", "owner@openroof.com", UserRole.USER);
        assignedAgentUser = createUser(301L, "Agent", "agent@openroof.com", UserRole.AGENT);
        assignedAgentProfile = createAgentProfile(401L, assignedAgentUser);
        property = createProperty(501L, "Casa Centro", ownerUser, assignedAgentProfile);
    }

    private User createUser(Long id, String name, String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .passwordHash("hash")
                .name(name)
                .role(role)
                .build();
        user.setId(id);
        return user;
    }

    private AgentProfile createAgentProfile(Long id, User user) {
        AgentProfile profile = AgentProfile.builder()
                .user(user)
                .build();
        profile.setId(id);
        return profile;
    }

    private Property createProperty(Long id, String title, User owner, AgentProfile agent) {
        Property p = Property.builder()
                .title(title)
                .owner(owner)
                .agent(agent)
                .build();
        p.setId(id);
        return p;
    }

    private VisitRequest createVisitRequest(Long id, Property property, User buyer, AgentProfile agent, LocalDateTime proposedAt, VisitRequestStatus status) {
        VisitRequest vr = VisitRequest.builder()
                .property(property)
                .buyer(buyer)
                .agent(agent)
                .proposedAt(proposedAt)
                .status(status)
                .buyerName(buyer.getName())
                .buyerEmail(buyer.getEmail())
                .buyerPhone(buyer.getPhone())
                .message("mensaje")
                .build();
        vr.setId(id);
        return vr;
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Usuario USER crea solicitud válida con fallback de datos")
        void createValidUserRequest_returnsPendingResponse() {
            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    property.getId(),
                    fixedNow.plusDays(2),
                    null,
                    null,
                    null,
                    "Quiero visitar mañana"
            );

            when(userRepository.findByEmail("buyer@openroof.com")).thenReturn(Optional.of(buyerUser));
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(visitRequestRepository.save(any(VisitRequest.class))).thenAnswer(invocation -> {
                VisitRequest saved = invocation.getArgument(0);
                saved.setId(901L);
                return saved;
            });

            VisitRequestResponse response = visitRequestService.create(request, "buyer@openroof.com");

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(901L);
            assertThat(response.status()).isEqualTo(VisitRequestStatus.PENDING);
            assertThat(response.propertyId()).isEqualTo(property.getId());
            assertThat(response.buyerId()).isEqualTo(buyerUser.getId());
            assertThat(response.buyerName()).isEqualTo(buyerUser.getName());
            assertThat(response.agentId()).isEqualTo(assignedAgentProfile.getId());
        }

        @Test
        @DisplayName("Usuario AGENT intenta crear solicitud y falla por rol")
        void createWithAgentRole_throwsBadRequest() {
            User agentAsCurrentUser = createUser(777L, "Current Agent", "agent-current@openroof.com", UserRole.AGENT);

            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    property.getId(),
                    fixedNow.plusDays(1),
                    "",
                    "",
                    "",
                    ""
            );

            when(userRepository.findByEmail("agent-current@openroof.com")).thenReturn(Optional.of(agentAsCurrentUser));

            assertThatThrownBy(() -> visitRequestService.create(request, "agent-current@openroof.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Solo un usuario puede crear una solicitud de visita");

            verify(propertyRepository, never()).findById(any());
            verify(visitRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si property.agent es null, usa assignment ACCEPTED más reciente")
        void createWhenPropertyHasNoAgent_usesAcceptedAssignmentAgent() {
            property.setAgent(null);

            PropertyAssignment acceptedAssignment = PropertyAssignment.builder()
                    .property(property)
                    .agent(assignedAgentProfile)
                    .assignedBy(ownerUser)
                    .status(AssignmentStatus.ACCEPTED)
                    .assignedAt(fixedNow)
                    .build();

            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    property.getId(),
                    fixedNow.plusDays(2),
                    null,
                    null,
                    null,
                    "Visita con agente asignado"
            );

            when(userRepository.findByEmail("buyer@openroof.com")).thenReturn(Optional.of(buyerUser));
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(propertyAssignmentRepository.findTopByProperty_IdAndStatusOrderByAssignedAtDesc(
                    property.getId(), AssignmentStatus.ACCEPTED)).thenReturn(Optional.of(acceptedAssignment));
            when(visitRequestRepository.save(any(VisitRequest.class))).thenAnswer(invocation -> {
                VisitRequest saved = invocation.getArgument(0);
                saved.setId(902L);
                return saved;
            });

            VisitRequestResponse response = visitRequestService.create(request, "buyer@openroof.com");

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(902L);
            assertThat(response.agentId()).isEqualTo(assignedAgentProfile.getId());
            assertThat(response.agentName()).isEqualTo(assignedAgentUser.getName());
        }
    }

    @Nested
    @DisplayName("accept()")
    class AcceptTests {

        @Test
        @DisplayName("Agente asignado acepta y crea Visit en estado CONFIRMED")
        void acceptByAssignedAgent_createsVisitAndMarksAccepted() {
            VisitRequest visitRequest = createVisitRequest(1001L, property, buyerUser, assignedAgentProfile, fixedNow.plusDays(3), VisitRequestStatus.PENDING);

            when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(assignedAgentUser));
            when(visitRequestRepository.findById(1001L)).thenReturn(Optional.of(visitRequest));
            when(agentProfileRepository.findByUser_Id(assignedAgentUser.getId())).thenReturn(Optional.of(assignedAgentProfile));
            when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
                Visit savedVisit = invocation.getArgument(0);
                savedVisit.setId(2001L);
                return savedVisit;
            });
            when(visitRequestRepository.save(any(VisitRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(clientInteractionService.recordVisitConfirmed(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class))).thenReturn(true);

            VisitRequestResponse response = visitRequestService.accept(1001L, "agent@openroof.com");

            assertThat(response.status()).isEqualTo(VisitRequestStatus.ACCEPTED);
            assertThat(response.visitId()).isEqualTo(2001L);

            ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
            verify(visitRepository).save(visitCaptor.capture());
            assertThat(visitCaptor.getValue().getStatus()).isEqualTo(VisitStatus.CONFIRMED);
            assertThat(visitCaptor.getValue().getProperty().getId()).isEqualTo(property.getId());
            verify(agentAgendaRepository, times(1)).save(any(com.openroof.openroof.model.interaction.AgentAgenda.class));
        }

        @Test
        @DisplayName("accept() crea AgentClient faltante y reintenta registrar interacción")
        void accept_createsMissingAgentClientAndRetriesInteractionRecording() {
            User buyer = createUser(20L, "Comprador", "buyer@openroof.com", UserRole.USER);
            buyer.setPhone("0991-000000");
            
            Property p = createProperty(30L, "Casa moderna", ownerUser, assignedAgentProfile);
            LocalDateTime proposedAt = fixedNow.plusDays(1);
            VisitRequest visitRequest = createVisitRequest(40L, p, buyer, assignedAgentProfile, proposedAt, VisitRequestStatus.PENDING);

            when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(assignedAgentUser));
            when(visitRequestRepository.findById(40L)).thenReturn(Optional.of(visitRequest));
            when(agentProfileRepository.findByUser_Id(assignedAgentUser.getId())).thenReturn(Optional.of(assignedAgentProfile));
            when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
                Visit visit = invocation.getArgument(0);
                visit.setId(99L);
                return visit;
            });
            when(clientInteractionService.recordVisitConfirmed(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class)))
                    .thenReturn(false, true);
            when(agentClientRepository.findByAgent_IdAndUser_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(agentClientRepository.save(any(AgentClient.class))).thenAnswer(invocation -> {
                AgentClient agentClient = invocation.getArgument(0);
                agentClient.setId(501L);
                return agentClient;
            });
            when(visitRequestRepository.save(visitRequest)).thenReturn(visitRequest);

            VisitRequestResponse response = visitRequestService.accept(40L, "agent@openroof.com");

            assertThat(response.status()).isEqualTo(VisitRequestStatus.ACCEPTED);
            assertThat(response.visitId()).isEqualTo(99L);
            verify(agentClientRepository).save(any(AgentClient.class));
            verify(clientInteractionService, times(2))
                    .recordVisitConfirmed(anyLong(), anyLong(), anyLong(), eq(proposedAt));
            verify(agentAgendaRepository, times(1)).save(any(com.openroof.openroof.model.interaction.AgentAgenda.class));
        }

        @Test
        @DisplayName("accept() no crea AgentClient cuando la interacción se registra en el primer intento")
        void accept_doesNotCreateAgentClientWhenInteractionIsRecordedImmediately() {
            User buyer = createUser(20L, "Comprador", "buyer@openroof.com", UserRole.USER);
            Property p = createProperty(30L, "Casa moderna", ownerUser, assignedAgentProfile);
            LocalDateTime proposedAt = fixedNow.plusDays(1);
            VisitRequest visitRequest = createVisitRequest(40L, p, buyer, assignedAgentProfile, proposedAt, VisitRequestStatus.PENDING);

            when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(assignedAgentUser));
            when(visitRequestRepository.findById(40L)).thenReturn(Optional.of(visitRequest));
            when(agentProfileRepository.findByUser_Id(assignedAgentUser.getId())).thenReturn(Optional.of(assignedAgentProfile));
            when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
                Visit visit = invocation.getArgument(0);
                visit.setId(99L);
                return visit;
            });
            when(clientInteractionService.recordVisitConfirmed(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class)))
                    .thenReturn(true);
            when(visitRequestRepository.save(visitRequest)).thenReturn(visitRequest);

            VisitRequestResponse response = visitRequestService.accept(40L, "agent@openroof.com");

            assertThat(response.status()).isEqualTo(VisitRequestStatus.ACCEPTED);
            verify(agentClientRepository, never()).findByAgent_IdAndUser_Id(any(), any());
            verify(agentClientRepository, never()).save(any(AgentClient.class));
            verify(clientInteractionService)
                    .recordVisitConfirmed(anyLong(), anyLong(), anyLong(), eq(proposedAt));
            verify(agentAgendaRepository, times(1)).save(any(com.openroof.openroof.model.interaction.AgentAgenda.class));
        }
    }

    @Nested
    @DisplayName("counterPropose()")
    class CounterProposeTests {

        @Test
        @DisplayName("Agente no asignado intenta contraofertar y falla")
        void counterProposeByOtherAgent_throwsBadRequest() {
            User otherAgentUser = createUser(999L, "Other Agent", "other-agent@openroof.com", UserRole.AGENT);
            AgentProfile otherAgentProfile = createAgentProfile(998L, otherAgentUser);

            VisitRequest visitRequest = createVisitRequest(3001L, property, buyerUser, assignedAgentProfile, fixedNow.plusDays(2), VisitRequestStatus.PENDING);

            CounterProposeRequest request = new CounterProposeRequest(fixedNow.plusDays(4), "nuevo horario");

            when(userRepository.findByEmail("other-agent@openroof.com")).thenReturn(Optional.of(otherAgentUser));
            when(visitRequestRepository.findById(3001L)).thenReturn(Optional.of(visitRequest));
            when(agentProfileRepository.findByUser_Id(otherAgentUser.getId())).thenReturn(Optional.of(otherAgentProfile));

            assertThatThrownBy(() -> visitRequestService.counterPropose(3001L, request, "other-agent@openroof.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("No eres el agente asignado a esta solicitud de visita");

            verify(visitRequestRepository, never()).save(any());
        }
    }
}
