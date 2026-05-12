package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.dashboard.TenantDashboardResponse;
import com.openroof.openroof.dto.dashboard.TenantLeaseResponse;
import com.openroof.openroof.dto.dashboard.TenantPaymentsResponse;
import com.openroof.openroof.dto.dashboard.TenantMaintenanceResponse;
import com.openroof.openroof.service.TenantDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant", description = "Endpoints para el tenant")
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resumen consolidado del dashboard del tenant")
    public ResponseEntity<ApiResponse<TenantDashboardResponse>> getTenantDashboard(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getDashboard(auth.getName())));
    }

    @GetMapping("/lease")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lease activo del tenant con documentos y contacto del landlord")
    public ResponseEntity<ApiResponse<TenantLeaseResponse>> getTenantLease(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getLease(auth.getName())));
    }

    @GetMapping("/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Historial de pagos del tenant")
    public ResponseEntity<ApiResponse<TenantPaymentsResponse>> getTenantPayments(
            Authentication auth,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getPayments(auth.getName(), pageable)));
    }

    @GetMapping("/maintenance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tickets de mantenimiento del tenant")
    public ResponseEntity<ApiResponse<TenantMaintenanceResponse>> getTenantMaintenance(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getMaintenance(auth.getName())));
    }
}
