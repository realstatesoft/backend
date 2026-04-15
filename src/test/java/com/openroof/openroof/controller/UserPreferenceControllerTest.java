package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.service.UserPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserPreferenceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserPreferenceService userPreferenceService;

    // Standard MockitoBeans for any controller test with security
    @MockitoBean private com.openroof.openroof.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private com.openroof.openroof.security.JwtService jwtService;
    @MockitoBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getOptions_isPublic() throws Exception {
        when(userPreferenceService.getPreferenceOptions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/preferences/options"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void save_withValidRequest_returnsOk() throws Exception {
        UserPreferenceRequestDTO request = new UserPreferenceRequestDTO(1L, List.of(1L, 2L), Collections.emptyList());
        UserPreferenceResponseDTO response = new UserPreferenceResponseDTO(1L, true, Collections.emptyList(), Collections.emptyList());

        when(userPreferenceService.saveOrUpdateUserPreferences(any())).thenReturn(response);

        mockMvc.perform(post("/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void getByUserId_requiresAuth() throws Exception {
        UserPreferenceResponseDTO response = new UserPreferenceResponseDTO(1L, true, Collections.emptyList(), Collections.emptyList());
        when(userPreferenceService.getUserPreferences(1L)).thenReturn(response);

        mockMvc.perform(get("/preferences/1"))
                .andExpect(status().isOk());
    }
}
