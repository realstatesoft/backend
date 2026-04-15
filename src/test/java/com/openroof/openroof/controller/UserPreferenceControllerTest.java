package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.UserPreferenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPreferenceController.class)
@Import(SecurityConfig.class)
class UserPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserPreferenceService userPreferenceService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

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
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"success\":false,\"message\":\"No autorizado\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /v1/preferences/options → 200 con opciones (Público)")
    void getOptions_returns200() throws Exception {
        PreferenceCategoryDTO cat = new PreferenceCategoryDTO(1L, "TEST", "Test", List.of(new PreferenceOptionDTO(1L, "Opt", "OPT", "TEST")));
        when(userPreferenceService.getPreferenceOptions()).thenReturn(List.of(cat));

        mockMvc.perform(get("/v1/preferences/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("TEST"))
                .andExpect(jsonPath("$.data[0].options[0].label").value("Opt"));
    }

    @Test
    @DisplayName("GET /v1/preferences/{userId} → 200 con preferencias")
    void getUserPreferences_returns200() throws Exception {
        UserPreferenceResponseDTO res = new UserPreferenceResponseDTO(1L, true, List.of(), List.of(new RangeDTO("PRICE", 100D, 200D)));
        when(userPreferenceService.getUserPreferences(1L)).thenReturn(res);

        mockMvc.perform(get("/v1/preferences/1").with(user("user@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.ranges[0].fieldName").value("PRICE"));
    }
    
    @Test
    @DisplayName("GET /v1/preferences/{userId} sin auth → 401")
    void getUserPreferences_unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/v1/preferences/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/preferences → 200 guardado exitoso")
    void savePreferences_returns200() throws Exception {
        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(1L, List.of(10L), Collections.emptyList());
        UserPreferenceResponseDTO res = new UserPreferenceResponseDTO(1L, true, List.of(new PreferenceOptionDTO(10L, "Opt", "OPT", "TEST")), Collections.emptyList());

        when(userPreferenceService.saveOrUpdateUserPreferences(any(UserPreferenceRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/v1/preferences")
                        .with(user("user@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true));
    }
    
    @Test
    @DisplayName("POST /v1/preferences payload invalido → 400")
    void savePreferences_invalidPayload_returns400() throws Exception {
        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(null, Collections.emptyList(), null);

        mockMvc.perform(post("/v1/preferences")
                        .with(user("user@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /v1/preferences/{userId} → 204")
    void deletePreferences_returns204() throws Exception {
        mockMvc.perform(delete("/v1/preferences/1").with(user("user@test.com")))
                .andExpect(status().isNoContent());
    }
}
