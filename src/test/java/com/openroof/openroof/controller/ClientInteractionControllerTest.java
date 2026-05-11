package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.agent.ClientInteractionResponse;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.config.SecurityHeadersFilter;
import com.openroof.openroof.security.AgentClientSecurity;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.ClientInteractionService;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientInteractionController.class)
@Import({ SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class, com.openroof.openroof.exception.GlobalExceptionHandler.class })
class ClientInteractionControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ClientInteractionService clientInteractionService;

    @MockitoBean(name = "agentClientSecurity")
    private AgentClientSecurity agentClientSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;

    @MockitoBean
    private SecurityHeadersFilter securityHeadersFilter;

    private static final String API_BASE = "/clients/1/interactions";

    @BeforeEach
    void setup() throws Exception {
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
                any(jakarta.servlet.ServletRequest.class),
                any(jakarta.servlet.ServletResponse.class),
                any(jakarta.servlet.FilterChain.class));

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"No autorizado\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private User getAgentUser() {
        User user = new User();
        user.setId(100L);
        user.setEmail("agent@test.com");
        user.setRole(UserRole.AGENT);
        user.setPasswordHash("");
        return user;
    }

    private org.springframework.security.core.Authentication getAgentAuth() {
        User user = getAgentUser();
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
    }

    private ClientInteractionResponse sampleResponse() {
        return new ClientInteractionResponse(
                10L,
                7L,
                "NOTE",
                null,
                "Cliente prefiere zona norte",
                "INFO_CAPTURED",
                "MANUAL",
                LocalDateTime.of(2026, 3, 23, 10, 30),
                LocalDateTime.of(2026, 3, 23, 10, 31));
    }

    @Nested
    @DisplayName("POST /api/crm/clients/{id}/interactions")
    class CreateTests {

        @Test
        @DisplayName("Crear interacción válida -> 201")
        void createValid_returns201() throws Exception {
            String json = """
                    {
                      "type": "NOTE",
                      "note": "Cliente prefiere zona norte",
                      "outcome": "INFO_CAPTURED"
                    }
                    """;

            when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
            when(clientInteractionService.create(eq(1L), any())).thenReturn(sampleResponse());

            mockMvc.perform(post(API_BASE)
                            .with(authentication(getAgentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.agentId").value(7))
                    .andExpect(jsonPath("$.data.type").value("NOTE"));
        }

        @Test
        @DisplayName("Crear interacción sin tipo -> 400")
        void createWithoutType_returns400() throws Exception {
            String json = """
                    {
                      "note": "Falta el tipo"
                    }
                    """;

            when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);

            mockMvc.perform(post(API_BASE)
                            .with(authentication(getAgentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Crear interacción con fecha futura -> 400")
        void createWithFutureOccurredAt_returns400() throws Exception {
            String json = """
                    {
                      "type": "NOTE",
                      "note": "Intento con fecha futura",
                      "outcome": "INFO_CAPTURED",
                      "occurredAt": "2099-03-23T10:30:00"
                    }
                    """;

            when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);

            mockMvc.perform(post(API_BASE)
                            .with(authentication(getAgentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Crear interacción sin acceso al cliente -> 403")
        void createWithoutAccess_returns403() throws Exception {
            String json = """
                    {
                      "type": "NOTE",
                      "note": "Cliente prefiere zona norte",
                      "outcome": "INFO_CAPTURED"
                    }
                    """;

            when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(false);

            mockMvc.perform(post(API_BASE)
                            .with(authentication(getAgentAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("GET /api/crm/clients/{id}/interactions filtra por tipo -> 200")
    void list_returns200() throws Exception {
        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
        when(clientInteractionService.list(eq(1L), eq(com.openroof.openroof.model.enums.InteractionType.NOTE), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(API_BASE)
                        .with(authentication(getAgentAuth()))
                        .param("type", "NOTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].agentId").value(7))
                .andExpect(jsonPath("$.data.content[0].type").value("NOTE"));
    }

    @Test
    @DisplayName("GET /api/crm/clients/{id}/interactions sin autenticación -> 401")
    void list_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(API_BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/crm/clients/{id}/interactions sin filtro usa pageable por defecto -> 200")
    void list_withoutType_returns200() throws Exception {
        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
        when(clientInteractionService.list(eq(1L), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(API_BASE)
                        .with(authentication(getAgentAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].source").value("MANUAL"));
    }

    @Test
    @DisplayName("PUT /api/crm/clients/{id}/interactions/{interactionId} actualiza -> 200")
    void update_returns200() throws Exception {
        String json = """
                {
                  "note": "Nota actualizada"
                }
                """;

        ClientInteractionResponse updated = new ClientInteractionResponse(
                10L,
                7L,
                "NOTE",
                null,
                "Nota actualizada",
                "INFO_CAPTURED",
                "MANUAL",
                LocalDateTime.of(2026, 3, 23, 10, 30),
                LocalDateTime.of(2026, 3, 23, 10, 45));

        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
        when(clientInteractionService.update(eq(1L), eq(10L), any())).thenReturn(updated);

        mockMvc.perform(put(API_BASE + "/10")
                        .with(authentication(getAgentAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.note").value("Nota actualizada"));
    }

    @Test
    @DisplayName("PUT /api/crm/clients/{id}/interactions/{interactionId} con fecha futura -> 400")
    void updateWithFutureOccurredAt_returns400() throws Exception {
        String json = """
                {
                  "occurredAt": "2099-03-23T10:30:00"
                }
                """;

        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);

        mockMvc.perform(put(API_BASE + "/10")
                        .with(authentication(getAgentAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/crm/clients/{id}/interactions/{interactionId} sin acceso -> 403")
    void updateWithoutAccess_returns403() throws Exception {
        String json = """
                {
                  "note": "Nota actualizada"
                }
                """;

        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(false);

        mockMvc.perform(put(API_BASE + "/10")
                        .with(authentication(getAgentAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/crm/clients/{id}/interactions/{interactionId} elimina -> 204")
    void delete_returns204() throws Exception {
        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(true);
        doNothing().when(clientInteractionService).delete(1L, 10L);

        mockMvc.perform(delete(API_BASE + "/10")
                        .with(authentication(getAgentAuth())))
                .andExpect(status().isNoContent());

        verify(clientInteractionService).delete(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/crm/clients/{id}/interactions/{interactionId} sin acceso -> 403")
    void deleteWithoutAccess_returns403() throws Exception {
        when(agentClientSecurity.canAccess(eq(1L), any())).thenReturn(false);

        mockMvc.perform(delete(API_BASE + "/10")
                        .with(authentication(getAgentAuth())))
                .andExpect(status().isForbidden());

        verify(clientInteractionService, never()).delete(any(), any());
    }
}
