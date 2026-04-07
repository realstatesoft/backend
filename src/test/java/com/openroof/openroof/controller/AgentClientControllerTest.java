package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.security.AgentClientSecurity;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.AgentClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(AgentClientController.class)
@Import({ SecurityConfig.class, com.openroof.openroof.exception.GlobalExceptionHandler.class })
class AgentClientControllerTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext context;

        @MockitoBean(name = "agentClientSecurity")
        private AgentClientSecurity agentClientSecurity;

        private final ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule());

        @MockitoBean
        private AgentClientService agentClientService;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        @MockitoBean
        private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        private static final String API_BASE = "/agent-clients";

        @BeforeEach
        void setup() throws Exception {
                // Setup MockMvc with Spring Security
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                doAnswer(invocation -> {
                        jakarta.servlet.ServletRequest req = invocation.getArgument(0);
                        jakarta.servlet.ServletResponse res = invocation.getArgument(1);
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(req, res);
                        return null;
                }).when(jwtAuthenticationFilter).doFilter(
                                any(jakarta.servlet.ServletRequest.class), any(jakarta.servlet.ServletResponse.class), any(jakarta.servlet.FilterChain.class));

                doAnswer(invocation -> {
                        jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
                        response.setStatus(401);
                        response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"success\":false,\"message\":\"No autorizado\"}");
                        return null;
                }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
        }

        // ─── Helpers ──────────────────────────────────────────────────

        private AgentClientResponse sampleResponse() {
                return new AgentClientResponse(
                                1L,
                                // Agent
                                10L, "Agente Test",
                                // User
                                20L, "Cliente Test", "cliente@test.com", "+595981000000",
                                // Estado
                                "ACTIVE", "MEDIUM",
                                // Tags
                                List.of("comprador", "urgente"),
                                // Contadores
                                0, 0,
                                // Rangos
                                new BigDecimal("100000"), new BigDecimal("300000"),
                                2, 4,
                                1, 2,
                                // Contacto
                                "EMAIL", null,
                                // Notas
                                "Cliente interesado en zona norte",
                                // Perfil detallado
                                LocalDate.of(1990, 1, 1), null, "Software Engineer", new BigDecimal("50000"),
                                "Calle Falsa 123", "FACEBOOK", 5,
                                List.of("Apartment"), List.of("Downtown"), List.of("Pool"),
                                false,
                                // Audit
                                LocalDateTime.now(), LocalDateTime.now());
        }

        private AgentClientSummaryResponse sampleSummary() {
                return new AgentClientSummaryResponse(
                1L,
                20L, "Cliente Test", "cliente@test.com", null,
                "ACTIVE", "MEDIUM",
                "INDIVIDUAL", null, LocalDateTime.now());
        }

        private User getAgentUser() {
                User u = new User();
                u.setId(100L);
                u.setEmail("agent@test.com");
                u.setRole(UserRole.AGENT);
                u.setPasswordHash("");
                return u;
        }

        private User getAdminUser() {
                User u = new User();
                u.setId(101L);
                u.setEmail("admin@test.com");
                u.setRole(UserRole.ADMIN);
                u.setPasswordHash("");
                return u;
        }

        private org.springframework.security.core.Authentication getAgentAuth() {
                User u = getAgentUser();
                return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(u, null,
                                u.getAuthorities());
        }

        private org.springframework.security.core.Authentication getAdminAuth() {
                User u = getAdminUser();
                return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(u, null,
                                u.getAuthorities());
        }

        // ═══════════════════════════════════════════════════════════════
        // CREATE
        // ═══════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("POST /agent-clients")
        class CreateTests {

                @Test
                @DisplayName("Crear relación agente-cliente válida → 201")
                void createValid_returns201() throws Exception {
                        String json = """
                                        {
                                            "agentId": 10,
                                            "userId": 20,
                                            "status": "ACTIVE",
                                            "priority": "HIGH",
                                            "notes": "Cliente interesado en zona norte"
                                        }
                                        """;

                        when(agentClientSecurity.canManageAgent(eq(10L), any())).thenReturn(true);
                        when(agentClientService.create(any(CreateAgentClientRequest.class)))
                                        .thenReturn(sampleResponse());

                        mockMvc.perform(post(API_BASE)
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.id").value(1))
                                        .andExpect(jsonPath("$.data.agentId").value(10))
                                        .andExpect(jsonPath("$.data.userId").value(20))
                                        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
                }

                @Test
                @DisplayName("Crear sin agentId → 400 validación")
                void createWithoutAgentId_returns400() throws Exception {
                        String json = """
                                        {
                                            "userId": 20
                                        }
                                        """;

                        mockMvc.perform(post(API_BASE)
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false));
                }

                @Test
                @DisplayName("Crear sin userId → 400 validación")
                void createWithoutUserId_returns400() throws Exception {
                        String json = """
                                        {
                                            "agentId": 10
                                        }
                                        """;

                        mockMvc.perform(post(API_BASE)
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false));
                }

                @Test
                @DisplayName("Crear relación duplicada → 400")
                void createDuplicate_returns400() throws Exception {
                        String json = """
                                        {
                                            "agentId": 10,
                                            "userId": 20
                                        }
                                        """;

                        when(agentClientSecurity.canManageAgent(eq(10L), any())).thenReturn(true);
                        when(agentClientService.create(any(CreateAgentClientRequest.class)))
                                        .thenThrow(new BadRequestException(
                                                        "Ya existe un registro de cliente para este agente y usuario"));

                        mockMvc.perform(post(API_BASE)
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false))
                                        .andExpect(jsonPath("$.message").value(containsString("Ya existe")));
                }

                @Test
                @DisplayName("Crear con agente inexistente → 404")
                void createWithNonExistentAgent_returns404() throws Exception {
                        String json = """
                                        {
                                            "agentId": 999,
                                            "userId": 20
                                        }
                                        """;

                        when(agentClientSecurity.canManageAgent(eq(999L), any())).thenReturn(true);
                        when(agentClientService.create(any(CreateAgentClientRequest.class)))
                                        .thenThrow(new ResourceNotFoundException("Agente no encontrado con ID: 999"));

                        mockMvc.perform(post(API_BASE)
                                        .with(authentication(getAdminAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.success").value(false))
                                        .andExpect(jsonPath("$.message").value(containsString("999")));
                }

                @Test
                @DisplayName("Crear con rangos de preferencia → 201")
                void createWithPreferenceRanges_returns201() throws Exception {
                        String json = """
                                        {
                                            "agentId": 10,
                                            "userId": 20,
                                            "minBudget": 100000,
                                            "maxBudget": 300000,
                                            "minBedrooms": 2,
                                            "maxBedrooms": 4,
                                            "preferredContactMethod": "WHATSAPP",
                                            "tags": ["comprador", "urgente"]
                                        }
                                        """;

                        when(agentClientSecurity.canManageAgent(eq(10L), any())).thenReturn(true);
                        when(agentClientService.create(any(CreateAgentClientRequest.class)))
                                        .thenReturn(sampleResponse());

                        mockMvc.perform(post(API_BASE)
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.minBudget").value(100000))
                                        .andExpect(jsonPath("$.data.maxBudget").value(300000))
                                        .andExpect(jsonPath("$.data.tags", hasSize(2)));

                        // Wait, there's no way to know if required values are null, just add checks
                        verify(agentClientService).create(argThat(req -> 
                                req != null &&
                                req.minBudget() != null && req.minBudget().equals(new BigDecimal("100000")) &&
                                req.maxBudget() != null && req.maxBudget().equals(new BigDecimal("300000")) &&
                                req.preferredContactMethod() != null && "WHATSAPP".equals(req.preferredContactMethod().name()) &&
                                req.tags() != null && req.tags().contains("comprador")
                        ));
                }
        }

        // ═══════════════════════════════════════════════════════════════
        // READ
        // ═══════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("GET /agent-clients")
        class ReadTests {

        @Test
        @DisplayName("Obtener por ID existente → 200")
        void getById_returns200() throws Exception {
            when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
            when(agentClientService.getById(1L)).thenReturn(sampleResponse());

            mockMvc.perform(get(API_BASE + "/1").with(authentication(getAgentAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.userName").value("Cliente Test"))
                    .andExpect(jsonPath("$.data.agentId").value(10));
        }

        @Test
        @DisplayName("Obtener por ID inexistente → 404")
        void getByInvalidId_returns404() throws Exception {
            when(agentClientSecurity.canAccess(eq(999L), any())).thenReturn(true);
            when(agentClientService.getById(999L))
                    .thenThrow(new ResourceNotFoundException(
                            "Cliente de agente no encontrado con ID: 999"));

            mockMvc.perform(get(API_BASE + "/999").with(authentication(getAgentAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }

        @Test
        @DisplayName("Listar clientes de un agente paginado → 200")
        void getByAgent_returns200WithPagination() throws Exception {
            when(agentClientSecurity.canManageAgent(eq(10L), any())).thenReturn(true);
            Page<AgentClientSummaryResponse> page = new PageImpl<>(
                    List.of(sampleSummary()), PageRequest.of(0, 10), 1);

            when(agentClientService.getByAgent(eq(10L), any())).thenReturn(page);

            mockMvc.perform(get(API_BASE + "/agent/10").with(authentication(getAgentAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].userName").value("Cliente Test"));
        }

        @Test
        @DisplayName("Listar clientes de agente sin clientes → 200 lista vacía")
        void getByAgent_emptyList_returns200() throws Exception {
            when(agentClientSecurity.canManageAgent(eq(10L), any())).thenReturn(true);
            Page<AgentClientSummaryResponse> emptyPage = new PageImpl<>(
                    List.of(), PageRequest.of(0, 10), 0);

            when(agentClientService.getByAgent(eq(10L), any())).thenReturn(emptyPage);

            mockMvc.perform(get(API_BASE + "/agent/10").with(authentication(getAgentAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.page.totalElements").value(0));
        }
        }

        // ═══════════════════════════════════════════════════════════════
        // UPDATE
        // ═══════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("PUT /agent-clients/{id}")
        class UpdateTests {

                @Test
                @DisplayName("Actualizar campos de cliente → 200")
                void updateValid_returns200() throws Exception {
                        String json = """
                                        {
                                            "status": "CONVERTED",
                                            "priority": "HIGH",
                                            "notes": "Cerró trato"
                                        }
                                        """;

                                                AgentClientResponse updated = new AgentClientResponse(
                                        1L, 10L, "Agente Test", 20L, "Cliente Test", "cliente@test.com",
                                        "+595981000000", "CONVERTED", "HIGH", List.of(), 0, 0,
                                        null, null, null, null, null, null,
                                        null, null, "Cerró trato",
                                        null, null, null, null, null, null, 0, List.of(), List.of(), List.of(),
                                        false,
                                        LocalDateTime.now(), LocalDateTime.now());

                        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
                        when(agentClientService.update(eq(1L), any(UpdateAgentClientRequest.class)))
                                        .thenReturn(updated);

                        mockMvc.perform(put(API_BASE + "/1")
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                                        .andExpect(jsonPath("$.data.priority").value("HIGH"))
                                        .andExpect(jsonPath("$.data.notes").value("Cerró trato"));
                }

                @Test
                @DisplayName("Actualizar ID inexistente → 404")
                void updateNonExistent_returns404() throws Exception {
                        String json = """
                                        {
                                            "status": "CONVERTED"
                                        }
                                        """;

                        when(agentClientSecurity.canAccess(eq(999L), any())).thenReturn(true);
                        when(agentClientService.update(eq(999L), any(UpdateAgentClientRequest.class)))
                                        .thenThrow(new ResourceNotFoundException(
                                                        "Cliente de agente no encontrado con ID: 999"));

                        mockMvc.perform(put(API_BASE + "/999")
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.success").value(false));
                }

                @Test
                @DisplayName("Update parcial — solo notas → 200")
                void updateOnlyNotes_returns200() throws Exception {
                        String json = """
                                        {
                                            "notes": "Nueva nota"
                                        }
                                        """;

                        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
                        when(agentClientService.update(eq(1L), any(UpdateAgentClientRequest.class)))
                                        .thenReturn(sampleResponse());

                        mockMvc.perform(put(API_BASE + "/1")
                                        .with(authentication(getAgentAuth()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true));

                        verify(agentClientService).update(eq(1L), argThat(req -> 
                                req != null && "Nueva nota".equals(req.notes())
                        ));
                }
        }

        // ═══════════════════════════════════════════════════════════════
        // DELETE
        // ═══════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("DELETE /agent-clients/{id}")
        class DeleteTests {

        @Test
        @DisplayName("Eliminar existente → 204")
        void delete_returns204() throws Exception {
            when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
            doNothing().when(agentClientService).delete(1L);

            mockMvc.perform(delete(API_BASE + "/1").with(authentication(getAgentAuth())))
                    .andExpect(status().isNoContent());

            verify(agentClientService).delete(1L);
        }

        @Test
        @DisplayName("Eliminar ID inexistente → 404")
        void deleteNonExistent_returns404() throws Exception {
            when(agentClientSecurity.canAccess(eq(999L), any())).thenReturn(true);
            doThrow(new ResourceNotFoundException(
                    "Cliente de agente no encontrado con ID: 999"))
                    .when(agentClientService).delete(999L);

            mockMvc.perform(delete(API_BASE + "/999").with(authentication(getAgentAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }
        }

        // ═══════════════════════════════════════════════════════════════
        // SECURITY / ACCESS CONTROL
        // ═══════════════════════════════════════════════════════════════

        @Test
        @DisplayName("Acceso sin autenticación → 401")
        void unauthenticatedAccess_returns401() throws Exception {
                mockMvc.perform(get(API_BASE + "/1"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Acceso autenticado pero prohibido (canAccess=false) → 403")
        void accessDenied_returns403() throws Exception {
                when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(false);

                mockMvc.perform(get(API_BASE + "/1").with(authentication(getAgentAuth())))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false));
                // .andExpect(jsonPath("$.message").value(containsString("Acceso denegado")));
        }

        @Test
        @DisplayName("Gestionar agente prohibido (canManageAgent=false) → 403")
        void manageAgentDenied_returns403() throws Exception {
                when(agentClientSecurity.canManageAgent(eq(10L), any())).thenReturn(false);

                mockMvc.perform(get(API_BASE + "/agent/10").with(authentication(getAgentAuth())))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false));
        }
}
