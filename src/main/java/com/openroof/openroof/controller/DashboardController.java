package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.dashboard.*;
import com.openroof.openroof.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Estadísticas y resúmenes para dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/agent/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Estadísticas del dashboard del agente")
    public ResponseEntity<ApiResponse<AgentDashboardStatsResponse>> getAgentStats(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getAgentStats(auth.getName())));
    }

    @GetMapping("/owner/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Estadísticas del dashboard del propietario")
    public ResponseEntity<ApiResponse<OwnerDashboardStatsResponse>> getOwnerStats(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getOwnerStats(auth.getName())));
    }

    @GetMapping("/sales")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista de ventas del usuario autenticado")
    public ResponseEntity<ApiResponse<List<SaleItemResponse>>> getSales(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getSales(auth.getName())));
    }

    @GetMapping("/sales/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resumen de ventas del usuario autenticado")
    public ResponseEntity<ApiResponse<SalesSummaryResponse>> getSalesSummary(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getSalesSummary(auth.getName())));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resumen de reportes del mercado")
    public ResponseEntity<ApiResponse<ReportsSummaryResponse>> getReportsSummary() {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getReportsSummary()));
    }

    @GetMapping("/agent/sales-performance")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Rendimiento de ventas comparativo año actual vs anterior")
    public ResponseEntity<ApiResponse<List<MonthlySalesData>>> getSalesPerformance(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getSalesPerformance(auth.getName())));
    }
}
