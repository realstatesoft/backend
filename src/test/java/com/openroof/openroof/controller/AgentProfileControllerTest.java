package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.AgentProfileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentProfileController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class AgentProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AgentProfileService agentProfileService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final String API_BASE = "/agents";

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private AgentProfileResponse sampleResponse() {
        return new AgentProfileResponse(
                10L, 1L, "Test Agent", "agent@test.com", "+1234567890", null,
                "Test Realty", "Experienced agent", 5, "LIC-001",
                BigDecimal.ZERO, 0,
                Collections.emptyList(), Collections.emptyList(),
                new AgentProfileResponse.AgentStatsDto(0, 0, 0, "$ 0"),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private AgentProfileSummaryResponse sampleSummary() {
        return new AgentProfileSummaryResponse(
                10L, 99L, "Test Agent", null, null, "Test Realty", 5, "LIC-001",
                BigDecimal.ZERO, 0, List.of("residencial", "casas")
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /agents")
    class CreateTests {

        @Test
        @DisplayName("Crear agente válido con ADMIN → 201")
        void createValidAgent_returns201() throws Exception {
            var request = new CreateAgentProfileRequest(
                    1L, "Test Realty", "Experienced agent", 5, "LIC-001",
                    null, null
            );

            when(agentProfileService.create(any(CreateAgentProfileRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(API_BASE)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.licenseNumber").value("LIC-001"))
                    .andExpect(jsonPath("$.data.companyName").value("Test Realty"));
        }

        @Test
        @DisplayName("Crear agente sin licencia → 201 (licencia opcional)")
        void createAgentWithoutLicense_returns201() throws Exception {
            String json = """
                    {
                        "userId": 1,
                        "companyName": "Company",
                        "bio": "Bio",
                        "experienceYears": 5
                    }
                    """;

            when(agentProfileService.create(any(CreateAgentProfileRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(API_BASE)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Crear agente sin userId → 400 validación")
        void createAgentWithoutUserId_returns400() throws Exception {
            String json = """
                    {
                        "companyName": "Company",
                        "licenseNumber": "LIC-001"
                    }
                    """;

            mockMvc.perform(post(API_BASE)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Crear agente con email duplicado (usuario ya tiene perfil) → 400")
        void createAgentDuplicateUser_returns400() throws Exception {
            var request = new CreateAgentProfileRequest(
                    1L, "Company", "Bio", 5, "LIC-001", null, null
            );

            when(agentProfileService.create(any(CreateAgentProfileRequest.class)))
                    .thenThrow(new BadRequestException("El usuario con ID 1 ya tiene un perfil de agente"));

            mockMvc.perform(post(API_BASE)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("ya tiene un perfil")));
        }

        @Test
        @DisplayName("Crear agente sin token → 401 (URL pública pero método protegido)")
        void createAgentWithoutAuth_returns403() throws Exception {
            var request = new CreateAgentProfileRequest(
                    1L, "Company", "Bio", 5, "LIC-001", null, null
            );

            mockMvc.perform(post(API_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Crear agente con rol no-admin → 403")
        void createAgentWithoutAdminRole_returns403() throws Exception {
            var request = new CreateAgentProfileRequest(
                    1L, "Company", "Bio", 5, "LIC-001", null, null
            );

            mockMvc.perform(post(API_BASE)
                            .with(user("buyer").roles("BUYER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /agents")
    class ReadTests {

        @Test
        @DisplayName("Listar agentes paginado → 200 con metadatos")
        void listAgents_returns200WithPagination() throws Exception {
            Page<AgentProfileSummaryResponse> page = new PageImpl<>(
                    List.of(sampleSummary()), PageRequest.of(0, 10), 1
            );

            when(agentProfileService.getAll(any())).thenReturn(page);

            mockMvc.perform(get(API_BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].userName").value("Test Agent"));
        }

        @Test
        @DisplayName("Obtener agente por ID existente → 200")
        void getAgentById_returns200() throws Exception {
            when(agentProfileService.getById(10L)).thenReturn(sampleResponse());

            mockMvc.perform(get(API_BASE + "/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.userName").value("Test Agent"));
        }

        @Test
        @DisplayName("Obtener agente por ID inexistente → 404")
        void getAgentByInvalidId_returns404() throws Exception {
            when(agentProfileService.getById(999L))
                    .thenThrow(new ResourceNotFoundException("Agente no encontrado con ID: 999"));

            mockMvc.perform(get(API_BASE + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }

        @Test
        @DisplayName("Buscar agentes con keyword → 200")
        void searchAgents_returns200() throws Exception {
            Page<AgentProfileSummaryResponse> page = new PageImpl<>(
                    List.of(sampleSummary()), PageRequest.of(0, 10), 1
            );

            when(agentProfileService.search(eq("Test"), any())).thenReturn(page);

            mockMvc.perform(get(API_BASE + "/search").param("q", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /agents/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Actualizar agente válido con ADMIN → 200")
        void updateValidAgent_returns200() throws Exception {
            var request = new UpdateAgentProfileRequest(
                    "New Company", "New bio", 10, null, null, null
            );

            AgentProfileResponse updated = new AgentProfileResponse(
                    10L, 1L, "Test Agent", "agent@test.com", "+1234567890", null,
                    "New Company", "New bio", 10, "LIC-001",
                    BigDecimal.ZERO, 0,
                    Collections.emptyList(), Collections.emptyList(),
                    new AgentProfileResponse.AgentStatsDto(0, 0, 0, "$ 0"),
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(agentProfileService.update(eq(10L), any(UpdateAgentProfileRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(put(API_BASE + "/10")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.companyName").value("New Company"))
                    .andExpect(jsonPath("$.data.experienceYears").value(10));
        }

        @Test
        @DisplayName("Actualizar agente sin auth → 401 (URL pública pero método protegido)")
        void updateAgentWithoutAuth_returns403() throws Exception {
            var request = new UpdateAgentProfileRequest(
                    "New Company", null, null, null, null, null
            );

            mockMvc.perform(put(API_BASE + "/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Actualizar agente con rol no-admin → 403")
        void updateAgentWithoutAdminRole_returns403() throws Exception {
            var request = new UpdateAgentProfileRequest(
                    "New Company", null, null, null, null, null
            );

            mockMvc.perform(put(API_BASE + "/10")
                            .with(user("agent").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /agents/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Eliminar agente con ADMIN → 204")
        void deleteAgent_returns204() throws Exception {
            doNothing().when(agentProfileService).delete(10L);

            mockMvc.perform(delete(API_BASE + "/10")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isNoContent());

            verify(agentProfileService).delete(10L);
        }

        @Test
        @DisplayName("Eliminar agente inexistente → 404")
        void deleteNonExistentAgent_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Agente no encontrado con ID: 999"))
                    .when(agentProfileService).delete(999L);

            mockMvc.perform(delete(API_BASE + "/999")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Eliminar agente sin token → 401 (URL pública pero método protegido)")
        void deleteAgentWithoutAuth_returns403() throws Exception {
            mockMvc.perform(delete(API_BASE + "/10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Eliminar agente con rol no-admin → 403")
        void deleteAgentWithoutAdminRole_returns403() throws Exception {
            mockMvc.perform(delete(API_BASE + "/10")
                            .with(user("buyer").roles("BUYER")))
                    .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SUGGESTED AGENTS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /agents/suggested")
    class SuggestedAgentsTests {

        @Test
        @DisplayName("Obtener agentes sugeridos sin filtros → 200 con lista")
        void getSuggestedAgents_returns200() throws Exception {
            when(agentProfileService.getSuggestedAgents(any(SuggestedAgentsRequest.class)))
                    .thenReturn(List.of(sampleSummary()));

            mockMvc.perform(get(API_BASE + "/suggested"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].userName").value("Test Agent"));
        }

        @Test
        @DisplayName("Obtener agentes sugeridos con propertyType → 200")
        void getSuggestedAgentsWithPropertyType_returns200() throws Exception {
            when(agentProfileService.getSuggestedAgents(any(SuggestedAgentsRequest.class)))
                    .thenReturn(List.of(sampleSummary()));

            mockMvc.perform(get(API_BASE + "/suggested")
                            .param("propertyType", "HOUSE")
                            .param("category", "SALE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Obtener agentes sugeridos con límite personalizado → 200")
        void getSuggestedAgentsWithLimit_returns200() throws Exception {
            when(agentProfileService.getSuggestedAgents(any(SuggestedAgentsRequest.class)))
                    .thenReturn(List.of(sampleSummary(), sampleSummary(), sampleSummary()));

            mockMvc.perform(get(API_BASE + "/suggested")
                            .param("limit", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)));
        }

        @Test
        @DisplayName("Obtener agentes sugeridos vacío → 200 con lista vacía")
        void getSuggestedAgentsEmpty_returns200() throws Exception {
            when(agentProfileService.getSuggestedAgents(any(SuggestedAgentsRequest.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get(API_BASE + "/suggested"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }
}
