package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.config.JacksonConfig;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Import({SecurityConfig.class, JacksonConfig.class})
class DashboardControllerTest {

    @Autowired
        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext context;

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

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(401);
            res.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"success\":false,\"message\":\"No autorizado\"}");
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

                // Ensure security filters are registered with MockMvc in this @WebMvcTest slice
                mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                                .build();
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
    @DisplayName("GET /dashboard/owner/stats → 200 con estadísticas de propietario")
    void getOwnerStats_returns200() throws Exception {
        OwnerDashboardStatsResponse response = new OwnerDashboardStatsResponse(
                CountStatItem.of(3, 0),
                CountStatItem.of(12, 0),
                CountStatItem.of(4, 0),
                CountStatItem.of(200, 0)
        );

        when(dashboardService.getOwnerStats("owner@test.com")).thenReturn(response);

        mockMvc.perform(get("/dashboard/owner/stats")
                        .with(user("owner@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.myProperties.value").value(3))
                .andExpect(jsonPath("$.data.totalVisits.value").value(12));
    }

    @Test
    @DisplayName("GET /dashboard/sales → 200 con lista de ventas y comisiones")
    void getSales_returns200() throws Exception {
        List<SaleItemResponse> sales = List.of(
                new SaleItemResponse(300L, "Casa en Las Mercedes", "Juan Pérez",
                        "Propietario Test", "SALE",
                        new BigDecimal("250000"), new BigDecimal("7500"),
                        new BigDecimal("3500"), "LISTING_AGENT",
                        LocalDate.of(2026, 1, 15), "SIGNED"),
                new SaleItemResponse(301L, "Apartamento moderno", "María García",
                        "Propietario Test", "RENT",
                        new BigDecimal("180000"), new BigDecimal("5400"),
                        new BigDecimal("0"), "BUYER_AGENT",
                        null, "DRAFT")
        );

        when(dashboardService.getSales("agent@test.com")).thenReturn(sales);

        mockMvc.perform(get("/dashboard/sales")
                        .with(user("agent@test.com")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].amount").value(250000))
                .andExpect(jsonPath("$.data[0].totalCommission").value(7500))
                .andExpect(jsonPath("$.data[0].myCommission").value(3500))
                .andExpect(jsonPath("$.data[0].myRole").value("LISTING_AGENT"))
                .andExpect(jsonPath("$.data[0].buyer").value("Juan Pérez"))
                .andExpect(jsonPath("$.data[0].status").value("SIGNED"))
                .andExpect(jsonPath("$.data[1].status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /dashboard/sales/summary → 200 con totales y datos mensuales")
    void getSalesSummary_returns200() throws Exception {
        SalesSummaryResponse summary = new SalesSummaryResponse(
                250000L, 7500L, 1, 2,
                List.of(
                        new SalesSummaryResponse.MonthlyDataPoint("Ene", 0L, 0L),
                        new SalesSummaryResponse.MonthlyDataPoint("Feb", 250000L, 7500L)
                )
        );

        when(dashboardService.getSalesSummary("agent@test.com")).thenReturn(summary);

        mockMvc.perform(get("/dashboard/sales/summary")
                        .with(user("agent@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSold").value(250000))
                .andExpect(jsonPath("$.data.myCommissions").value(7500))
                .andExpect(jsonPath("$.data.signedContracts").value(1))
                .andExpect(jsonPath("$.data.activeContracts").value(2))
                .andExpect(jsonPath("$.data.monthlyData").isArray());
    }

    @Test
    @DisplayName("GET /dashboard/agent/stats sin autenticación → 401")
    void getAgentStats_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/dashboard/agent/stats"))
                .andExpect(status().isUnauthorized());
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
