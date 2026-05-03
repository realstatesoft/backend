package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.settings.AdminSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAdminCommissionsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminPropertiesRequest;
import com.openroof.openroof.dto.settings.UpdateAdminReservationsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminSystemRequest;
import com.openroof.openroof.service.AdminSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Settings", description = "Configuración general del sistema")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping
    @Operation(summary = "Obtener todas las configuraciones del sistema")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(adminSettingsService.getSettings()));
    }

    @PutMapping("/commissions")
    @Operation(summary = "Actualizar configuración de comisiones")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateCommissions(
            @Valid @RequestBody UpdateAdminCommissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSettingsService.updateCommissions(request),
                "Configuración de comisiones actualizada"));
    }

    @PutMapping("/reservations")
    @Operation(summary = "Actualizar configuración de reservas")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateReservations(
            @Valid @RequestBody UpdateAdminReservationsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSettingsService.updateReservations(request),
                "Configuración de reservas actualizada"));
    }

    @PutMapping("/properties")
    @Operation(summary = "Actualizar configuración de propiedades")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateProperties(
            @Valid @RequestBody UpdateAdminPropertiesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSettingsService.updateProperties(request),
                "Configuración de propiedades actualizada"));
    }

    @PutMapping("/system")
    @Operation(summary = "Actualizar configuración del sistema")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateSystem(
            @Valid @RequestBody UpdateAdminSystemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSettingsService.updateSystem(request),
                "Configuración del sistema actualizada"));
    }
}
