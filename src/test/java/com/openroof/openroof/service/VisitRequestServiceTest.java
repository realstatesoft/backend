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

    @InjectMocks
    private VisitRequestService visitRequestService;

    private User buyerUser;
    private User ownerUser;
    private User assignedAgentUser;
    private AgentProfile assignedAgentProfile;
    private Property property;

    @BeforeEach
    void setUp() {
        buyerUser = User.builder().email("buyer@openroof.com").passwordHash("hash").name("Buyer").role(UserRole.USER).build();
        buyerUser.setId(101L);

        ownerUser = User.builder().email("owner@openroof.com").passwordHash("hash").name("Owner").role(UserRole.USER).build();
        ownerUser.setId(201L);

        assignedAgentUser = User.builder().email("agent@openroof.com").passwordHash("hash").name("Agent").role(UserRole.AGENT).build();
        assignedAgentUser.setId(301L);

        assignedAgentProfile = AgentProfile.builder().user(assignedAgentUser).build();
        assignedAgentProfile.setId(401L);

        property = Property.builder().title("Casa Centro").owner(ownerUser).agent(assignedAgentProfile).build();
        property.setId(501L);
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Usuario USER crea solicitud válida con fallback de datos")
        void createValidUserRequest_returnsPendingResponse() {
            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    property.getId(),
                    LocalDateTime.now().plusDays(2),
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
            User agentAsCurrentUser = User.builder()
                    .email("agent-current@openroof.com")
                    .passwordHash("hash")
                    .name("Current Agent")
                    .role(UserRole.AGENT)
                    .build();
            agentAsCurrentUser.setId(777L);

            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    property.getId(),
                    LocalDateTime.now().plusDays(1),
                    "",
                    "",
                    "",
                    ""
            );

            when(userRepository.findByEmail("agent-current@openroof.com")).thenReturn(Optional.of(agentAsCurrentUser));

            assertThatThrownBy(() -> visitRequestService.create(request, "agent-current@openroof.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Solo un usuario puede crear una solicitud");

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
                    .assignedAt(LocalDateTime.now())
                    .build();

            CreateVisitRequestRequest request = new CreateVisitRequestRequest(
                    property.getId(),
                    LocalDateTime.now().plusDays(2),
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
            VisitRequest visitRequest = VisitRequest.builder()
                    .property(property)
                    .buyer(buyerUser)
                    .agent(assignedAgentProfile)
                    .proposedAt(LocalDateTime.now().plusDays(3))
                    .status(VisitRequestStatus.PENDING)
                    .buyerName("Buyer")
                    .buyerEmail("buyer@openroof.com")
                    .buyerPhone("555-1111")
                    .message("mensaje")
                    .build();
            visitRequest.setId(1001L);

            when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(assignedAgentUser));
            when(visitRequestRepository.findById(1001L)).thenReturn(Optional.of(visitRequest));
            when(agentProfileRepository.findByUser_Id(assignedAgentUser.getId())).thenReturn(Optional.of(assignedAgentProfile));
            when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
                Visit savedVisit = invocation.getArgument(0);
                savedVisit.setId(2001L);
                return savedVisit;
            });
            when(visitRequestRepository.save(any(VisitRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            VisitRequestResponse response = visitRequestService.accept(1001L, "agent@openroof.com");

            assertThat(response.status()).isEqualTo(VisitRequestStatus.ACCEPTED);
            assertThat(response.visitId()).isEqualTo(2001L);

            ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
            verify(visitRepository).save(visitCaptor.capture());
            assertThat(visitCaptor.getValue().getStatus()).isEqualTo(VisitStatus.CONFIRMED);
            assertThat(visitCaptor.getValue().getProperty().getId()).isEqualTo(property.getId());
        }

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

    @Nested
    @DisplayName("counterPropose()")
    class CounterProposeTests {

        @Test
        @DisplayName("Agente no asignado intenta contraofertar y falla")
        void counterProposeByOtherAgent_throwsBadRequest() {
            User otherAgentUser = User.builder().email("other-agent@openroof.com").passwordHash("hash").name("Other Agent").role(UserRole.AGENT).build();
            otherAgentUser.setId(999L);
            AgentProfile otherAgentProfile = AgentProfile.builder().user(otherAgentUser).build();
            otherAgentProfile.setId(998L);

            VisitRequest visitRequest = VisitRequest.builder()
                    .property(property)
                    .buyer(buyerUser)
                    .agent(assignedAgentProfile)
                    .proposedAt(LocalDateTime.now().plusDays(2))
                    .status(VisitRequestStatus.PENDING)
                    .build();
            visitRequest.setId(3001L);

            CounterProposeRequest request = new CounterProposeRequest(LocalDateTime.now().plusDays(4), "nuevo horario");

            when(userRepository.findByEmail("other-agent@openroof.com")).thenReturn(Optional.of(otherAgentUser));
            when(visitRequestRepository.findById(3001L)).thenReturn(Optional.of(visitRequest));
            when(agentProfileRepository.findByUser_Id(otherAgentUser.getId())).thenReturn(Optional.of(otherAgentProfile));

            assertThatThrownBy(() -> visitRequestService.counterPropose(3001L, request, "other-agent@openroof.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No eres el agente asignado");

            verify(visitRequestRepository, never()).save(any());
        }
    }
}
