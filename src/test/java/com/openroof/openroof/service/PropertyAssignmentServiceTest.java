package com.openroof.openroof.service;

import com.openroof.openroof.dto.property.AssignPropertyRequest;
import com.openroof.openroof.dto.property.PropertyAssignmentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyAssignment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.PropertyAssignmentRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyAssignmentServiceTest {

    @Mock
    private PropertyAssignmentRepository assignmentRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private AgentProfileRepository agentProfileRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PropertyAssignmentService propertyAssignmentService;

    private User ownerUser;
    private User assignedAgentUser;
    private AgentProfile assignedAgentProfile;
    private Property property;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder()
                .email("owner@openroof.com")
                .passwordHash("hash")
                .name("Owner")
                .role(UserRole.USER)
                .build();
        ownerUser.setId(10L);

        assignedAgentUser = User.builder()
                .email("agent@openroof.com")
                .passwordHash("hash")
                .name("Agent")
                .role(UserRole.AGENT)
                .build();
        assignedAgentUser.setId(20L);

        assignedAgentProfile = AgentProfile.builder().user(assignedAgentUser).build();
        assignedAgentProfile.setId(30L);

        property = Property.builder()
                .title("Apartamento Norte")
                .owner(ownerUser)
                .agent(assignedAgentProfile)
                .build();
        property.setId(40L);
    }

    @Nested
    @DisplayName("assign()")
    class AssignTests {

        @Test
        @DisplayName("Owner asigna agente válido y retorna response pendiente")
        void assignValid_returnsPendingResponse() {
            AssignPropertyRequest request = new AssignPropertyRequest(assignedAgentProfile.getId());

            when(userRepository.findByEmail("owner@openroof.com")).thenReturn(Optional.of(ownerUser));
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(agentProfileRepository.findById(assignedAgentProfile.getId())).thenReturn(Optional.of(assignedAgentProfile));
            when(assignmentRepository.findActiveByPropertyAndAgent(
                    property.getId(),
                    assignedAgentProfile.getId(),
                    List.of(AssignmentStatus.PENDING, AssignmentStatus.ACCEPTED)
            )).thenReturn(Optional.empty());
            when(assignmentRepository.save(any(PropertyAssignment.class))).thenAnswer(invocation -> {
                PropertyAssignment saved = invocation.getArgument(0);
                saved.setId(111L);
                return saved;
            });

            PropertyAssignmentResponse response = propertyAssignmentService.assign(property.getId(), request, "owner@openroof.com");

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(111L);
            assertThat(response.status()).isEqualTo(AssignmentStatus.PENDING);
            assertThat(response.propertyId()).isEqualTo(property.getId());
            assertThat(response.agentProfileId()).isEqualTo(assignedAgentProfile.getId());
            assertThat(response.agentUserId()).isEqualTo(assignedAgentUser.getId());
        }

        @Test
        @DisplayName("Owner no puede crear asignación duplicada activa")
        void assignDuplicateActive_throwsBadRequest() {
            AssignPropertyRequest request = new AssignPropertyRequest(assignedAgentProfile.getId());
            PropertyAssignment existing = PropertyAssignment.builder()
                    .property(property)
                    .agent(assignedAgentProfile)
                    .assignedBy(ownerUser)
                    .status(AssignmentStatus.PENDING)
                    .build();
            existing.setId(222L);

            when(userRepository.findByEmail("owner@openroof.com")).thenReturn(Optional.of(ownerUser));
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(agentProfileRepository.findById(assignedAgentProfile.getId())).thenReturn(Optional.of(assignedAgentProfile));
            when(assignmentRepository.findActiveByPropertyAndAgent(
                    property.getId(),
                    assignedAgentProfile.getId(),
                    List.of(AssignmentStatus.PENDING, AssignmentStatus.ACCEPTED)
            )).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> propertyAssignmentService.assign(property.getId(), request, "owner@openroof.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Ya existe una asignación activa");

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("accept()")
    class AcceptTests {

        @Test
        @DisplayName("Agente asignado acepta solicitud pendiente")
        void acceptPendingByAssignedAgent_returnsAccepted() {
            PropertyAssignment assignment = PropertyAssignment.builder()
                    .property(property)
                    .agent(assignedAgentProfile)
                    .assignedBy(ownerUser)
                    .status(AssignmentStatus.PENDING)
                    .build();
            assignment.setId(333L);

            when(userRepository.findByEmail("agent@openroof.com")).thenReturn(Optional.of(assignedAgentUser));
            when(assignmentRepository.findById(333L)).thenReturn(Optional.of(assignment));
            when(propertyRepository.findById(40L)).thenReturn(Optional.of(property));
            when(assignmentRepository.save(any(PropertyAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PropertyAssignmentResponse response = propertyAssignmentService.accept(333L, "agent@openroof.com");

            assertThat(response.status()).isEqualTo(AssignmentStatus.ACCEPTED);
            assertThat(response.id()).isEqualTo(333L);
        }
    }

    @Nested
    @DisplayName("getMyAssignments()")
    class GetMyAssignmentsTests {

        @Test
        @DisplayName("Usuario sin perfil de agente recibe error")
        void getMyAssignmentsWithoutAgentProfile_throwsBadRequest() {
            User simpleUser = User.builder()
                    .email("simple@openroof.com")
                    .passwordHash("hash")
                    .name("Simple")
                    .role(UserRole.USER)
                    .build();
            simpleUser.setId(444L);

            when(userRepository.findByEmail("simple@openroof.com")).thenReturn(Optional.of(simpleUser));
            when(agentProfileRepository.findByUser_Id(444L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyAssignmentService.getMyAssignments("simple@openroof.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No tienes un perfil de agente asociado");

            verify(assignmentRepository, never()).findByAgent_Id(any());
        }
    }
}
