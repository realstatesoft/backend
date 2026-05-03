package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.settings.UpdateUserSettingsRequest;
import com.openroof.openroof.dto.settings.UserSettingsResponse;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.model.enums.NotifyChannel;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.UserSettingsService;
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

@WebMvcTest(UserSettingsController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class UserSettingsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserSettingsService userSettingsService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE = "/settings/user";

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

    private UserSettingsResponse sampleSettings() {
        return new UserSettingsResponse(true, true, true, NotifyChannel.BOTH, true, true);
    }

    @Nested
    @DisplayName("GET /settings/user")
    class GetSettings {

        @Test
        @DisplayName("USER obtiene sus ajustes con defaults → 200")
        void userGetsSettings_returns200() throws Exception {
            when(userSettingsService.getSettings(eq("user@test.com")))
                    .thenReturn(sampleSettings());

            mockMvc.perform(get(BASE).with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.notifyPriceDrop").value(true))
                    .andExpect(jsonPath("$.data.notifyChannel").value("BOTH"))
                    .andExpect(jsonPath("$.data.profileVisibleToAgents").value(true))
                    .andExpect(jsonPath("$.data.allowDirectContact").value(true));
        }

        @Test
        @DisplayName("Sin autenticación → 401")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rol AGENT → 403")
        void agentRole_returns403() throws Exception {
            mockMvc.perform(get(BASE).with(user("agent").roles("AGENT")))
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
    @DisplayName("PUT /settings/user")
    class UpdateSettings {

        @Test
        @DisplayName("USER con payload válido → 200 con ajustes actualizados")
        void userUpdatesSettings_returns200() throws Exception {
            UserSettingsResponse updated = new UserSettingsResponse(
                    false, true, false, NotifyChannel.EMAIL, false, true);
            when(userSettingsService.updateSettings(eq("user@test.com"), any()))
                    .thenReturn(updated);

            var body = new UpdateUserSettingsRequest(
                    false, true, false, NotifyChannel.EMAIL, false, true);

            mockMvc.perform(put(BASE)
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.notifyPriceDrop").value(false))
                    .andExpect(jsonPath("$.data.notifyChannel").value("EMAIL"))
                    .andExpect(jsonPath("$.data.profileVisibleToAgents").value(false))
                    .andExpect(jsonPath("$.message").value("Configuración actualizada"));
        }

        @Test
        @DisplayName("notifyChannel nulo → 400 (campo obligatorio)")
        void nullNotifyChannel_returns400() throws Exception {
            String body = """
                    {"notifyPriceDrop": true, "notifyNewMatch": true,
                     "notifyMessages": true, "notifyChannel": null,
                     "profileVisibleToAgents": true, "allowDirectContact": true}
                    """;

            mockMvc.perform(put(BASE)
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("notifyChannel con valor inválido → 400")
        void invalidNotifyChannel_returns400() throws Exception {
            String body = """
                    {"notifyPriceDrop": true, "notifyNewMatch": true,
                     "notifyMessages": true, "notifyChannel": "SMOKE_SIGNAL",
                     "profileVisibleToAgents": true, "allowDirectContact": true}
                    """;

            mockMvc.perform(put(BASE)
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
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
        @DisplayName("Rol AGENT → 403")
        void agentRole_returns403() throws Exception {
            mockMvc.perform(put(BASE)
                            .with(user("agent").roles("AGENT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
}
