package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.service.UserPreferenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class, com.openroof.openroof.test.SliceSecurityBeans.class})
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

    @BeforeEach
    void setup() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

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
