package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.agent.AgentSpecialtyResponse;
import com.openroof.openroof.dto.agent.CreateAgentSpecialtyRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.AgentSpecialtyService;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentSpecialtyController.class)
@Import(SecurityConfig.class)
class AgentSpecialtyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AgentSpecialtyService agentSpecialtyService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final String BASE = "/agents/specialties";

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Nested
    @DisplayName("GET /agents/specialties")
    class GetAllTests {

        @Test
        @DisplayName("Listar especialidades → 200 con lista")
        void listSpecialties_returns200() throws Exception {
            when(agentSpecialtyService.getAll()).thenReturn(List.of(
                    new AgentSpecialtyResponse(1L, "A"),
                    new AgentSpecialtyResponse(2L, "B")
            ));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].name").value("A"));
        }
    }

    @Nested
    @DisplayName("POST /agents/specialties")
    class CreateTests {

        @Test
        @DisplayName("Crear especialidad con ADMIN → 201")
        void createWithAdmin_returns201() throws Exception {
            var req = new CreateAgentSpecialtyRequest("Residential");
            when(agentSpecialtyService.create(any(CreateAgentSpecialtyRequest.class)))
                    .thenReturn(new AgentSpecialtyResponse(5L, "Residential"));

            mockMvc.perform(post(BASE)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(5))
                    .andExpect(jsonPath("$.data.name").value("Residential"));
        }

        @Test
        @DisplayName("Crear especialidad duplicada → 400")
        void createDuplicate_returns400() throws Exception {
            var req = new CreateAgentSpecialtyRequest("Residential");
            when(agentSpecialtyService.create(any())).thenThrow(new BadRequestException("La especialidad ya existe"));

            mockMvc.perform(post(BASE)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("ya existe")));
        }

        @Test
        @DisplayName("Crear especialidad sin ADMIN → 403")
        void createWithoutAdmin_returns403() throws Exception {
            var req = new CreateAgentSpecialtyRequest("Residential");

            mockMvc.perform(post(BASE)
                            .with(user("user").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }
}
