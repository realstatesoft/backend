package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.dashboard.*;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.DashboardService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

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
    }

    @Test
    @DisplayName("GET /dashboard/agent/stats → 200 con estadísticas")
    void getAgentStats_returns200() throws Exception {
        AgentDashboardStatsResponse response = new AgentDashboardStatsResponse(
                CountStatItem.of(10, 0),
                CountStatItem.of(5, 0),
                CountStatItem.of(3, 0),
                MoneyStatItem.of(new BigDecimal("1000"), 0)
        );

        when(dashboardService.getAgentStats("agent@test.com")).thenReturn(response);

        mockMvc.perform(get("/dashboard/agent/stats")
                        .with(user("agent@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeClients.value").value(10));
    }

    @Test
    @DisplayName("GET /dashboard/reports/summary → 200 con resumen")
    void getReportsSummary_returns200() throws Exception {
        ReportsSummaryResponse response = new ReportsSummaryResponse(
                new ReportsSummaryResponse.MarketMetrics(0, 0, 10, 0, 0, 0, 50, 0),
                List.of(), List.of()
        );

        when(dashboardService.getReportsSummary()).thenReturn(response);

        mockMvc.perform(get("/dashboard/reports/summary")
                        .with(user("any@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketMetrics.closingRate").value(50));
    }
}
