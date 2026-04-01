package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.dashboard.MonthlySalesData;
import com.openroof.openroof.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/agent/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Analíticas del dashboard del agente")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/sales-performance")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Obtener rendimiento de ventas/alquileres comparativo (año actual vs anterior)")
    public ResponseEntity<ApiResponse<List<MonthlySalesData>>> getSalesPerformance(Principal principal) {

        List<MonthlySalesData> data = dashboardService.getSalesPerformance(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
