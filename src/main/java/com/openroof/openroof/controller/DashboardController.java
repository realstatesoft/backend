package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.dashboard.*;
import com.openroof.openroof.dto.dashboard.funnel.*;
import com.openroof.openroof.service.ConversionFunnelService;
import com.openroof.openroof.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Estadísticas y resúmenes para dashboards")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ConversionFunnelService conversionFunnelService;

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

    @GetMapping("/owner/overview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vista general del dashboard del propietario (Estadísticas + Propiedades + Firmas Pendientes)")
    public ResponseEntity<ApiResponse<OwnerDashboardOverviewResponse>> getOwnerOverview(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getOwnerOverview(auth.getName())));
    }

    @GetMapping("/tenant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resumen consolidado para el dashboard del inquilino")
    public ResponseEntity<ApiResponse<TenantDashboardResponse>> getTenantDashboard(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getTenantDashboard(auth.getName())));
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

    @GetMapping("/agent/conversion-funnel/summary")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Embudo de conversión del agente (vistas, visitas, ofertas, ventas firmadas)")
    public ResponseEntity<ApiResponse<ConversionFunnelSummaryResponse>> getConversionFunnelSummary(
            Authentication auth,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "MONTH") ConversionFunnelGranularity granularity,
            @RequestParam(defaultValue = "true") boolean comparePrevious,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        ConversionFunnelSummaryResponse body = conversionFunnelService.getSummary(
                auth.getName(), from, to, granularity, comparePrevious,
                locationId, propertyType, minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/agent/conversion-funnel/top-properties")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Propiedades del agente ordenadas por rendimiento en el embudo")
    public ResponseEntity<ApiResponse<PropertyFunnelPageResponse>> getConversionFunnelTopProperties(
            Authentication auth,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        PropertyFunnelPageResponse body = conversionFunnelService.getTopProperties(
                auth.getName(), from, to, page, size, locationId, propertyType, minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

}
