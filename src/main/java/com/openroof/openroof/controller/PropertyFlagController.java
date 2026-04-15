package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.flag.CreateFlagRequest;
import com.openroof.openroof.dto.flag.FlagResponse;
import com.openroof.openroof.dto.flag.ResolveFlagRequest;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.PropertyFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints para reportar y gestionar propiedades fraudulentas o ilegales.
 *
 * Base URL (con context-path /api): /api/properties/{propertyId}/flags
 *                                   /api/flags
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Property Flags", description = "Reporte y gestión de propiedades fraudulentas o ilegales")
public class PropertyFlagController {

    private final PropertyFlagService propertyFlagService;

    // ─── Endpoints anidados bajo /properties/{propertyId}/flags ───

    /**
     * POST /properties/{propertyId}/flags
     * Reporta una propiedad. Requiere autenticación (USER o ADMIN).
     */
    @PostMapping("/properties/{propertyId}/flags")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reportar una propiedad como fraudulenta, spam u otro tipo")
    public ResponseEntity<ApiResponse<FlagResponse>> createFlag(
            @Parameter(description = "ID de la propiedad a reportar") @PathVariable Long propertyId,
            @Valid @RequestBody CreateFlagRequest request,
            @AuthenticationPrincipal User currentUser) {

        FlagResponse response = propertyFlagService.createFlag(propertyId, request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Propiedad reportada exitosamente"));
    }

    /**
     * GET /properties/{propertyId}/flags
     * Lista los flags activos de una propiedad. Endpoint público.
     */
    @GetMapping("/properties/{propertyId}/flags")
    @Operation(summary = "Obtener los reportes activos de una propiedad")
    public ResponseEntity<ApiResponse<List<FlagResponse>>> getActiveFlags(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId) {

        List<FlagResponse> flags = propertyFlagService.getActiveFlagsByProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(flags));
    }

    /**
     * GET /properties/{propertyId}/flags/count
     * Cuenta los flags activos de una propiedad. Endpoint público.
     */
    @GetMapping("/properties/{propertyId}/flags/count")
    @Operation(summary = "Contar los reportes activos de una propiedad")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countActiveFlags(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId) {

        long count = propertyFlagService.countActiveFlags(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    // ─── Endpoints raíz /flags (solo ADMIN) ───────────────────────

    /**
     * GET /flags
     * Lista todos los flags activos del sistema. Solo ADMIN.
     */
    @GetMapping("/flags")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los reportes activos del sistema (solo ADMIN)")
    public ResponseEntity<ApiResponse<List<FlagResponse>>> getAllActiveFlags() {

        List<FlagResponse> flags = propertyFlagService.getAllActiveFlags();
        return ResponseEntity.ok(ApiResponse.ok(flags));
    }

    /**
     * PUT /flags/{id}/resolve
     * Resuelve un flag. Solo ADMIN.
     */
    @PutMapping("/flags/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolver un reporte de propiedad (solo ADMIN)")
    public ResponseEntity<ApiResponse<FlagResponse>> resolveFlag(
            @Parameter(description = "ID del reporte") @PathVariable Long id,
            @Valid @RequestBody ResolveFlagRequest request,
            @AuthenticationPrincipal User admin) {

        FlagResponse response = propertyFlagService.resolveFlag(id, request, admin);
        return ResponseEntity.ok(ApiResponse.ok(response, "Reporte resuelto exitosamente"));
    }
}
