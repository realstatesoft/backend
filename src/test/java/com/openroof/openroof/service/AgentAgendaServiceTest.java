package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentAgendaResponse;
import com.openroof.openroof.dto.agent.CreateAgentAgendaRequest;
import com.openroof.openroof.dto.agent.UpdateAgentAgendaRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentAgendaMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.EventType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentAgendaRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentAgendaServiceTest {

    @Mock private AgentAgendaRepository agentAgendaRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private AgentAgendaMapper agentAgendaMapper;

    @InjectMocks
    private AgentAgendaService agentAgendaService;

    private User testUser;
    private String testEmail = "test@test.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(testEmail)
                .role(UserRole.AGENT)
                .build();
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {
        @Test
        @DisplayName("Crear evento válido → exitoso")
        void createValidEvent_success() {
            var request = new CreateAgentAgendaRequest(
                    EventType.VISIT, "Test", "Desc",
                    LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Loc", "Notes", null
            );

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.findByUser_Email(testEmail)).thenReturn(Optional.of(new AgentProfile()));
            
            AgentAgenda entity = new AgentAgenda();
            when(agentAgendaMapper.toEntity(any(), any(), any(), any())).thenReturn(entity);
            when(agentAgendaRepository.save(entity)).thenReturn(entity);
            
            agentAgendaService.create(request, testEmail);

            verify(agentAgendaRepository).save(entity);
        }

        @Test
        @DisplayName("Fecha fin antes que inicio → lanza BadRequestException")
        void invalidDates_throwsBadRequest() {
            var request = new CreateAgentAgendaRequest(
                    EventType.OTHER, "Test", null,
                    LocalDateTime.now().plusHours(1), LocalDateTime.now(), null, null, null
            );

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> agentAgendaService.create(request, testEmail))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("posterior");
        }
    }

    @Nested
    @DisplayName("getAgendaForMonth()")
    class ReadTests {
        @Test
        @DisplayName("Obtener agenda del mes → llama al repositorio con parámetros correctos")
        void getAgendaForMonth_callsRepository() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusMonths(1);
            
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentAgendaRepository.findByUserAndMonthOverlap(1L, start, end))
                    .thenReturn(Collections.emptyList());

            agentAgendaService.getAgendaForMonth(testEmail, start, end);

            verify(agentAgendaRepository).findByUserAndMonthOverlap(1L, start, end);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {
        @Test
        @DisplayName("Eliminar evento existente → llama al repositorio delete")
        void deleteExistingEvent_callsRepo() {
            AgentAgenda event = new AgentAgenda();
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentAgendaRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(event));

            agentAgendaService.delete(10L, testEmail);

            verify(agentAgendaRepository).delete(event);
        }

        @Test
        @DisplayName("Eliminar evento inexistente → lanza ResourceNotFoundException")
        void deleteNonExistent_throwsNotFound() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentAgendaRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentAgendaService.delete(99L, testEmail))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
