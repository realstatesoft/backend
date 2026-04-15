package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
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
        Long userId = 1L;
        User currentUser = User.builder().email("user@test.com").role(UserRole.USER).build();
        currentUser.setId(userId);

        UserPreferenceResponseDTO res = new UserPreferenceResponseDTO(userId, true, List.of(), List.of(new RangeDTO("PRICE", 100D, 200D)));
        when(userPreferenceService.getUserPreferences(userId)).thenReturn(res);

        mockMvc.perform(get("/v1/preferences/" + userId).with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    @DisplayName("GET /v1/preferences/{userId} de otro usuario → 403 Forbidden")
    void getUserPreferences_otherUser_returns403() throws Exception {
        Long targetUserId = 2L;
        User currentUser = User.builder().email("user@test.com").role(UserRole.USER).build();
        currentUser.setId(1L); // My ID is 1, targeting 2

        mockMvc.perform(get("/v1/preferences/" + targetUserId).with(user(currentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/preferences/{userId} de otro usuario como ADMIN → 200")
    void getUserPreferences_otherUserAsAdmin_returns200() throws Exception {
        Long targetUserId = 2L;
        User adminUser = User.builder().email("admin@test.com").role(UserRole.ADMIN).build();
        adminUser.setId(99L);

        UserPreferenceResponseDTO res = new UserPreferenceResponseDTO(targetUserId, true, List.of(), List.of());
        when(userPreferenceService.getUserPreferences(targetUserId)).thenReturn(res);

        mockMvc.perform(get("/v1/preferences/" + targetUserId).with(user(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /v1/preferences → 200 guardado exitoso")
    void savePreferences_returns200() throws Exception {
        Long userId = 1L;
        User currentUser = User.builder().email("user@test.com").role(UserRole.USER).build();
        currentUser.setId(userId);

        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(userId, List.of(10L), Collections.emptyList());
        UserPreferenceResponseDTO res = new UserPreferenceResponseDTO(userId, true, List.of(new PreferenceOptionDTO(10L, "Opt", "OPT", "TEST")), Collections.emptyList());

        when(userPreferenceService.saveOrUpdateUserPreferences(any(UserPreferenceRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/v1/preferences")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /v1/preferences con rango inválido → 400")
    void savePreferences_invalidRange_returns400() throws Exception {
        Long userId = 1L;
        User currentUser = User.builder().email("user@test.com").role(UserRole.USER).build();
        currentUser.setId(userId);

        RangeDTO invalidRange = new RangeDTO("PRICE", 500D, 100D); // min > max
        UserPreferenceRequestDTO req = new UserPreferenceRequestDTO(userId, List.of(10L), List.of(invalidRange));

        mockMvc.perform(post("/v1/preferences")
                        .with(user(currentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    @Test
    @DisplayName("DELETE /v1/preferences/{userId} → 204")
    void deletePreferences_returns204() throws Exception {
        Long userId = 1L;
        User currentUser = User.builder().email("user@test.com").role(UserRole.USER).build();
        currentUser.setId(userId);

        mockMvc.perform(delete("/v1/preferences/" + userId).with(user(currentUser)))
                .andExpect(status().isNoContent());
    }
}
