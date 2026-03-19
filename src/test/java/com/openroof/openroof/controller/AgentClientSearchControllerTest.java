package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.agent.AgentClientSearchRequest;
import com.openroof.openroof.dto.agent.AgentClientSummaryResponse;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.security.AgentClientSecurity;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.AgentClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentClientController.class)
@Import({ SecurityConfig.class, AgentClientSearchControllerTest.SecurityTestConfig.class })
class AgentClientSearchControllerTest {

    @TestConfiguration
    static class SecurityTestConfig {
        @Bean(name = "agentClientSecurity")
        public AgentClientSecurity agentClientSecurity() {
            return mock(AgentClientSecurity.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AgentClientSecurity agentClientSecurity;

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

    private static final String API_BASE = "/clients";

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private User getAgentUser() {
        User u = new User();
        u.setId(100L);
        u.setEmail("agent@test.com");
        u.setRole(UserRole.AGENT);
        return u;
    }

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken getAgentAuth() {
        User u = getAgentUser();
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
    }

    @Test
    @DisplayName("GET /clients -> Busca con filtros")
    void searchClients_returns200() throws Exception {
        User agentUser = getAgentUser();
        when(agentClientSecurity.isAgent(any())).thenReturn(true);
        when(agentClientService.getAgentIdByUser(agentUser.getId())).thenReturn(10L);
        
        AgentClientSummaryResponse summary = new AgentClientSummaryResponse(
                1L, 20L, "Client Name", "client@email.com", 
                "ACTIVE", "MEDIUM", "INDIVIDUAL", null, LocalDateTime.now());
        
        Page<AgentClientSummaryResponse> page = new PageImpl<>(List.of(summary));
        
        when(agentClientService.searchClients(eq(10L), any(AgentClientSearchRequest.class), any())).thenReturn(page);

        mockMvc.perform(get(API_BASE)
                .with(authentication(getAgentAuth()))
                .param("q", "Client")
                .param("status", "ACTIVE")
                .param("clientType", "INDIVIDUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userName").value("Client Name"))
                .andExpect(jsonPath("$.data.content[0].clientType").value("INDIVIDUAL"));
    }

    @Test
    @DisplayName("GET /clients/export -> Exporta a CSV")
    void exportClients_returnsCsv() throws Exception {
        User agentUser = getAgentUser();
        when(agentClientSecurity.isAgent(any())).thenReturn(true);
        when(agentClientService.getAgentIdByUser(agentUser.getId())).thenReturn(10L);
        
        String csvContent = "ID,Name,Email,Status,Priority,ClientType,CreatedAt\n1,Client Name,client@email.com,ACTIVE,MEDIUM,INDIVIDUAL,2026-03-19T00:00:00";
        
        when(agentClientService.exportClientsToCsv(eq(10L), any(AgentClientSearchRequest.class))).thenReturn(csvContent);

        mockMvc.perform(get(API_BASE + "/export")
                .with(authentication(getAgentAuth())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=clients.csv"))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(csvContent));
    }
}
