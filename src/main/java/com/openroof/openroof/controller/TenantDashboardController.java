package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.dashboard.*;
import com.openroof.openroof.service.TenantDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant", description = "Endpoints para el tenant")
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;
    private final com.openroof.openroof.service.LeasePdfService leasePdfService;

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resumen consolidado del dashboard del tenant")
    public ResponseEntity<ApiResponse<TenantDashboardResponse>> getTenantDashboard(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getDashboard(auth.getName())));
    }

    @GetMapping("/lease")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Leases activos del tenant con documentos y contacto del landlord")
    public ResponseEntity<ApiResponse<Page<TenantLeaseResponse>>> getTenantLeases(
            Authentication auth,
            @PageableDefault(size = 5, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getLeases(auth.getName(), pageable)));
    }

    @GetMapping("/lease/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalle de un lease específico del tenant")
    public ResponseEntity<ApiResponse<TenantLeaseResponse>> getTenantLeaseById(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getLeaseById(auth.getName(), id)));
    }

    @GetMapping(value = "/lease/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar PDF del contrato de arrendamiento")
    public ResponseEntity<byte[]> downloadLeasePdf(@PathVariable Long id, Authentication auth) {
        byte[] pdfBytes = leasePdfService.generatePdf(id, auth.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                .filename("contrato-arrendamiento-" + id + ".pdf")
                .build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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
    public ResponseEntity<ApiResponse<TenantMaintenanceResponse>> getTenantMaintenance(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.getMaintenance(auth.getName(), pageable)));
    }

    @PostMapping("/maintenance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear solicitud de mantenimiento")
    public ResponseEntity<ApiResponse<TenantMaintenanceTicketItem>> createMaintenanceRequest(
            Authentication auth,
            @Valid @RequestBody CreateMaintenanceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                tenantDashboardService.createMaintenanceRequest(auth.getName(), request)));
    }

    @PostMapping("/maintenance/{id}/rate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calificar solicitud de mantenimiento")
    public ResponseEntity<ApiResponse<Void>> rateMaintenanceRequest(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody RateMaintenanceRequest request) {
        tenantDashboardService.rateMaintenanceRequest(auth.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

