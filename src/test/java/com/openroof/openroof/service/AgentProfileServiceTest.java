package com.openroof.openroof.service;

import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.AgentProfileMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.agent.AgentSpecialty;
import com.openroof.openroof.model.enums.SocialMediaPlatform;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.AgentSpecialtyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentProfileServiceTest {

    @Mock
    private AgentProfileRepository agentProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AgentSpecialtyRepository agentSpecialtyRepository;
    @Mock
    private AgentProfileMapper agentProfileMapper;

    @InjectMocks
    private AgentProfileService agentProfileService;

    private User testUser;
    private AgentProfile testAgent;
    private AgentProfileResponse testResponse;
    private AgentProfileSummaryResponse testSummary;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("agent@test.com")
                .passwordHash("hashed")
                .name("Test Agent")
                .phone("+1234567890")
                .role(UserRole.BUYER)
                .build();
        testUser.setId(1L);

        testAgent = AgentProfile.builder()
                .user(testUser)
                .companyName("Test Realty")
                .bio("Experienced agent")
                .experienceYears(5)
                .licenseNumber("LIC-001")
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .specialties(new ArrayList<>())
                .socialMedia(new ArrayList<>())
                .build();
        testAgent.setId(10L);
        testAgent.setCreatedAt(LocalDateTime.now());
        testAgent.setUpdatedAt(LocalDateTime.now());

        testResponse = new AgentProfileResponse(
                10L, 1L, "Test Agent", "agent@test.com", "+1234567890", null,
                "Test Realty", "Experienced agent", 5, "LIC-001",
                BigDecimal.ZERO, 0,
                Collections.emptyList(), Collections.emptyList(),
                testAgent.getCreatedAt(), testAgent.getUpdatedAt()
        );

        testSummary = new AgentProfileSummaryResponse(
                10L, "Test Agent", null, "Test Realty", 5, "LIC-001",
                BigDecimal.ZERO, 0, List.of("residencial", "casas")
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Crear agente válido → retorna respuesta con datos correctos")
        void createValidAgent_returnsResponse() {
            var request = new CreateAgentProfileRequest(
                    1L, "Test Realty", "Experienced agent", 5, "LIC-001",
                    null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.existsByUser_Id(1L)).thenReturn(false);
            when(agentProfileMapper.toEntity(eq(request), eq(testUser), anyList())).thenReturn(testAgent);
            when(agentProfileRepository.save(testAgent)).thenReturn(testAgent);
            when(agentProfileMapper.toResponse(testAgent)).thenReturn(testResponse);

            AgentProfileResponse result = agentProfileService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.licenseNumber()).isEqualTo("LIC-001");
            assertThat(result.companyName()).isEqualTo("Test Realty");
            verify(userRepository).save(testUser); // role changed to AGENT
            verify(agentProfileRepository).save(testAgent);
        }

        @Test
        @DisplayName("Crear agente sin número de licencia → el servicio lo acepta (campo opcional)")
        void createAgentWithoutLicense_serviceAcceptsNullLicense() {
            var request = new CreateAgentProfileRequest(
                    1L, "Test Realty", "Bio", 5, null, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.existsByUser_Id(1L)).thenReturn(false);
            when(agentProfileMapper.toEntity(any(), any(), anyList())).thenReturn(testAgent);
            when(agentProfileRepository.save(any())).thenReturn(testAgent);
            when(agentProfileMapper.toResponse(any())).thenReturn(testResponse);

            assertThatCode(() -> agentProfileService.create(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Crear agente con usuario inexistente → 404")
        void createAgentUserNotFound_throwsNotFound() {
            var request = new CreateAgentProfileRequest(
                    999L, "Company", "Bio", 5, "LIC-002", null, null
            );

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentProfileService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Crear agente con usuario que ya tiene perfil → 400")
        void createAgentDuplicateUser_throwsBadRequest() {
            var request = new CreateAgentProfileRequest(
                    1L, "Company", "Bio", 5, "LIC-003", null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.existsByUser_Id(1L)).thenReturn(true);

            assertThatThrownBy(() -> agentProfileService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ya tiene un perfil de agente");
        }

        @Test
        @DisplayName("Crear agente con especialidades inválidas → 400")
        void createAgentInvalidSpecialties_throwsBadRequest() {
            var request = new CreateAgentProfileRequest(
                    1L, "Company", "Bio", 5, "LIC-004",
                    List.of(1L, 2L, 999L), null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.existsByUser_Id(1L)).thenReturn(false);
            when(agentSpecialtyRepository.findAllById(new LinkedHashSet<>(List.of(1L, 2L, 999L))))
                    .thenReturn(List.of(new AgentSpecialty(), new AgentSpecialty())); // only 2 found

            assertThatThrownBy(() -> agentProfileService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("especialidades no fueron encontradas");
        }

        @Test
        @DisplayName("Crear agente con redes sociales → se incluyen en la entidad")
        void createAgentWithSocialMedia_success() {
            var socialMedia = List.of(
                    new AgentSocialMediaDto(SocialMediaPlatform.LINKEDIN, "https://linkedin.com/test")
            );
            var request = new CreateAgentProfileRequest(
                    1L, "Company", "Bio", 5, "LIC-005", null, socialMedia
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.existsByUser_Id(1L)).thenReturn(false);
            when(agentProfileMapper.toEntity(eq(request), eq(testUser), anyList())).thenReturn(testAgent);
            when(agentProfileRepository.save(testAgent)).thenReturn(testAgent);
            when(agentProfileMapper.toResponse(testAgent)).thenReturn(testResponse);

            AgentProfileResponse result = agentProfileService.create(request);
            assertThat(result).isNotNull();
            verify(agentProfileMapper).toEntity(eq(request), eq(testUser), anyList());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Obtener agente existente → 200 con datos")
        void getExistingAgent_returnsResponse() {
            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(agentProfileMapper.toResponse(testAgent)).thenReturn(testResponse);

            AgentProfileResponse result = agentProfileService.getById(10L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.userName()).isEqualTo("Test Agent");
        }

        @Test
        @DisplayName("Obtener agente inexistente → 404")
        void getNonExistentAgent_throwsNotFound() {
            when(agentProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentProfileService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Listar agentes paginado → retorna página con metadatos correctos")
        void listAgentsPaginated_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AgentProfile> page = new PageImpl<>(List.of(testAgent), pageable, 1);

            when(agentProfileRepository.findAllWithUser(pageable)).thenReturn(page);
            when(agentProfileMapper.toSummaryResponse(testAgent)).thenReturn(testSummary);

            Page<AgentProfileSummaryResponse> result = agentProfileService.getAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).userName()).isEqualTo("Test Agent");
        }

        @Test
        @DisplayName("Listar agentes vacío → retorna página vacía")
        void listAgentsEmpty_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AgentProfile> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(agentProfileRepository.findAllWithUser(pageable)).thenReturn(page);

            Page<AgentProfileSummaryResponse> result = agentProfileService.getAll(pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("Buscar con keyword → retorna resultados filtrados")
        void searchWithKeyword_returnsFiltered() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AgentProfile> page = new PageImpl<>(List.of(testAgent), pageable, 1);

            when(agentProfileRepository.searchByKeyword("Test", pageable)).thenReturn(page);
            when(agentProfileMapper.toSummaryResponse(testAgent)).thenReturn(testSummary);

            Page<AgentProfileSummaryResponse> result = agentProfileService.search("Test", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(agentProfileRepository).searchByKeyword("Test", pageable);
        }

        @Test
        @DisplayName("Buscar con keyword vacío → retorna todos")
        void searchWithEmptyKeyword_returnsAll() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AgentProfile> page = new PageImpl<>(List.of(testAgent), pageable, 1);

            when(agentProfileRepository.findAllWithUser(pageable)).thenReturn(page);
            when(agentProfileMapper.toSummaryResponse(testAgent)).thenReturn(testSummary);

            Page<AgentProfileSummaryResponse> result = agentProfileService.search("", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(agentProfileRepository).findAllWithUser(pageable);
            verify(agentProfileRepository, never()).searchByKeyword(anyString(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Actualizar agente con cambios válidos → retorna datos actualizados")
        void updateValidAgent_returnsUpdatedResponse() {
            var request = new UpdateAgentProfileRequest(
                    "New Company", "New bio", 10, null, null, null
            );

            AgentProfileResponse updatedResponse = new AgentProfileResponse(
                    10L, 1L, "Test Agent", "agent@test.com", "+1234567890", null,
                    "New Company", "New bio", 10, "LIC-001",
                    BigDecimal.ZERO, 0,
                    Collections.emptyList(), Collections.emptyList(),
                    testAgent.getCreatedAt(), testAgent.getUpdatedAt()
            );

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(agentProfileRepository.save(testAgent)).thenReturn(testAgent);
            when(agentProfileMapper.toResponse(testAgent)).thenReturn(updatedResponse);

            AgentProfileResponse result = agentProfileService.update(10L, request);

            assertThat(result.companyName()).isEqualTo("New Company");
            assertThat(result.bio()).isEqualTo("New bio");
            assertThat(result.experienceYears()).isEqualTo(10);
            verify(agentProfileMapper).updateEntity(testAgent, request);
        }

        @Test
        @DisplayName("Actualizar agente inexistente → 404")
        void updateNonExistentAgent_throwsNotFound() {
            var request = new UpdateAgentProfileRequest(
                    "New Company", null, null, null, null, null
            );

            when(agentProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentProfileService.update(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Actualizar especialidades del agente → se reemplazan")
        void updateAgentSpecialties_replacesSpecialties() {
            AgentSpecialty spec = AgentSpecialty.builder().name("Residential").build();
            spec.setId(1L);

            var request = new UpdateAgentProfileRequest(
                    null, null, null, null, List.of(1L), null
            );

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(agentSpecialtyRepository.findAllById(new LinkedHashSet<>(List.of(1L)))).thenReturn(List.of(spec));
            when(agentProfileRepository.save(testAgent)).thenReturn(testAgent);
            when(agentProfileMapper.toResponse(testAgent)).thenReturn(testResponse);

            agentProfileService.update(10L, request);

            assertThat(testAgent.getSpecialties()).containsExactly(spec);
        }

        @Test
        @DisplayName("Actualizar redes sociales del agente → se reemplazan")
        void updateAgentSocialMedia_replacesSocialMedia() {
            var socialMedia = List.of(
                    new AgentSocialMediaDto(SocialMediaPlatform.INSTAGRAM, "https://instagram.com/test")
            );
            var request = new UpdateAgentProfileRequest(
                    null, null, null, null, null, socialMedia
            );

            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));
            when(agentProfileRepository.save(testAgent)).thenReturn(testAgent);
            when(agentProfileMapper.toResponse(testAgent)).thenReturn(testResponse);

            agentProfileService.update(10L, request);

            verify(agentProfileMapper).replaceSocialMedia(testAgent, socialMedia);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Eliminar agente existente → soft delete exitoso")
        void deleteExistingAgent_softDeletes() {
            when(agentProfileRepository.findById(10L)).thenReturn(Optional.of(testAgent));

            agentProfileService.delete(10L);

            assertThat(testAgent.getDeletedAt()).isNotNull();
            verify(agentProfileRepository).save(testAgent);
        }

        @Test
        @DisplayName("Eliminar agente inexistente → 404")
        void deleteNonExistentAgent_throwsNotFound() {
            when(agentProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentProfileService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }
}
