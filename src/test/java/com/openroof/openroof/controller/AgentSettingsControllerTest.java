package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.settings.AgentSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAgentSettingsRequest;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.AgentSettingsService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentSettingsController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class AgentSettingsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AgentSettingsService agentSettingsService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE = "/settings/agent";

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(
                    inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(inv -> {
            ((jakarta.servlet.http.HttpServletResponse) inv.getArgument(1)).setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());
    }

    private AgentSettingsResponse sampleSettings() {
        return new AgentSettingsResponse(true, true, false, true, 50);
    }

    @Nested
    @DisplayName("GET /settings/agent")
    class GetSettings {

        @Test
        @DisplayName("AGENT obtiene sus ajustes → 200")
        void agentGetsSettings_returns200() throws Exception {
            when(agentSettingsService.getSettings(eq("agent@test.com")))
                    .thenReturn(sampleSettings());

            mockMvc.perform(get(BASE).with(user("agent@test.com").roles("AGENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.autoAssignLeads").value(true))
                    .andExpect(jsonPath("$.data.notifyVisitRequest").value(false))
                    .andExpect(jsonPath("$.data.workRadiusKm").value(50));
        }

        @Test
        @DisplayName("Sin autenticación → 401")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rol USER → 403")
        void userRole_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(user("buyer").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Rol ADMIN → 403")
        void adminRole_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /settings/agent")
    class UpdateSettings {

        @Test
        @DisplayName("AGENT con payload válido → 200 con ajustes actualizados")
        void agentUpdatesSettings_returns200() throws Exception {
            when(agentSettingsService.updateSettings(eq("agent@test.com"), any()))
                    .thenReturn(new AgentSettingsResponse(false, true, true, false, 100));

            var body = new UpdateAgentSettingsRequest(false, true, true, false, 100);

            mockMvc.perform(put(BASE)
                            .with(user("agent@test.com").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.autoAssignLeads").value(false))
                    .andExpect(jsonPath("$.data.workRadiusKm").value(100))
                    .andExpect(jsonPath("$.message").value("Configuración actualizada"));
        }

        @Test
        @DisplayName("workRadiusKm = 0 → 400 (mínimo es 1)")
        void zeroWorkRadius_returns400() throws Exception {
            String body = """
                    {"autoAssignLeads": true, "notifyNewLead": true,
                     "notifyVisitRequest": true, "notifyNewOffer": true, "workRadiusKm": 0}
                    """;

            mockMvc.perform(put(BASE)
                            .with(user("agent@test.com").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("workRadiusKm nulo es aceptado → 200")
        void nullWorkRadius_returns200() throws Exception {
            when(agentSettingsService.updateSettings(any(), any()))
                    .thenReturn(new AgentSettingsResponse(true, true, true, true, null));

            String body = """
                    {"autoAssignLeads": true, "notifyNewLead": true,
                     "notifyVisitRequest": true, "notifyNewOffer": true}
                    """;

            mockMvc.perform(put(BASE)
                            .with(user("agent@test.com").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.workRadiusKm").doesNotExist());
        }

        @Test
        @DisplayName("Sin autenticación → 401")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(put(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rol USER → 403")
        void userRole_returns403() throws Exception {
            mockMvc.perform(put(BASE)
                            .with(user("buyer").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
}
