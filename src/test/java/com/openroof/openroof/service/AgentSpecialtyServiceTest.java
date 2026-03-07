package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.AgentSpecialtyResponse;
import com.openroof.openroof.dto.agent.CreateAgentSpecialtyRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.repository.AgentSpecialtyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentSpecialtyServiceTest {

    @Mock
    private AgentSpecialtyRepository agentSpecialtyRepository;

    @InjectMocks
    private AgentSpecialtyService agentSpecialtyService;

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Crear especialidad válida → retorna response con id y nombre")
        void createValid_returnsResponse() {
            var req = new CreateAgentSpecialtyRequest("Residential");

            when(agentSpecialtyRepository.existsByName("Residential")).thenReturn(false);

            AgentSpecialty saved = AgentSpecialty.builder().name("Residential").build();
            saved.setId(5L);
            when(agentSpecialtyRepository.save(any())).thenReturn(saved);

            AgentSpecialtyResponse resp = agentSpecialtyService.create(req);

            assertThat(resp).isNotNull();
            assertThat(resp.id()).isEqualTo(5L);
            assertThat(resp.name()).isEqualTo("Residential");
            verify(agentSpecialtyRepository).save(any(AgentSpecialty.class));
        }

        @Test
        @DisplayName("Crear especialidad duplicada → lanza BadRequestException")
        void createDuplicate_throwsBadRequest() {
            var req = new CreateAgentSpecialtyRequest("Residential");
            when(agentSpecialtyRepository.existsByName("Residential")).thenReturn(true);

            assertThatThrownBy(() -> agentSpecialtyService.create(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ya existe");
            verify(agentSpecialtyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Listar especialidades → retorna lista de responses")
        void getAll_returnsList() {
            AgentSpecialty a = AgentSpecialty.builder().name("A").build();
            a.setId(1L);
            AgentSpecialty b = AgentSpecialty.builder().name("B").build();
            b.setId(2L);

            when(agentSpecialtyRepository.findAll()).thenReturn(List.of(a, b));

            var list = agentSpecialtyService.getAll();

            assertThat(list).hasSize(2);
            assertThat(list).extracting(AgentSpecialtyResponse::name).containsExactly("A", "B");
        }
    }
}
