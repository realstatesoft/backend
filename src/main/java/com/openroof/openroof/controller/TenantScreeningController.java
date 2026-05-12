package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.screening.CreateScreeningRequest;
import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.service.TenantScreeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Endpoints para gestionar tenant screenings con provider INTERNAL (carga manual).
 */
@RestController
@RequestMapping("/tenant-screenings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Screenings", description = "Screenings de inquilinos (carga manual interna)")
public class TenantScreeningController {

    private final TenantScreeningService screeningService;

    // ─── POST /tenant-screenings (ADMIN) ──────────────────────────────────────

    @Operation(
            summary = "Crear screening interno",
            description = "Crea un tenant screening con provider=INTERNAL para una rental application. "
                    + "Setea expires_at = now + 90 días."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantScreeningResponse>> create(
            @Valid @RequestBody CreateScreeningRequest request
    ) {
        TenantScreeningResponse screening = screeningService.createScreening(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(screening.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.ok(screening, "Screening creado exitosamente"));
    }

    // ─── PUT /tenant-screenings/{id} (ADMIN) ──────────────────────────────────

    @Operation(
            summary = "Cargar resultados manualmente",
            description = "Permite al admin ingresar resultados del screening. Si no se envía "
                    + "recommendation, se recalcula automáticamente según las reglas de negocio."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantScreeningResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScreeningRequest request
    ) {
        TenantScreeningResponse screening = screeningService.updateScreeningResults(id, request);
        return ResponseEntity.ok(ApiResponse.ok(screening, "Screening actualizado exitosamente"));
    }

    // ─── POST /tenant-screenings/{id}/recommendation (ADMIN) ──────────────────

    @Operation(
            summary = "Recalcular recomendación",
            description = "Aplica las reglas: evictions ⇒ REJECT; ratio>=3 y background CLEAR ⇒ APPROVE; resto ⇒ REVIEW."
    )
    @PostMapping("/{id}/recommendation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantScreeningResponse>> recalculate(@PathVariable Long id) {
        TenantScreeningResponse screening = screeningService.calculateRecommendation(id);
        return ResponseEntity.ok(ApiResponse.ok(screening, "Recomendación recalculada"));
    }

    // ─── GET /tenant-screenings/{id} (ADMIN o AGENT asignado) ─────────────────

    @Operation(summary = "Obtener screening por id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @screeningSecurity.hasReadAccess(#id, principal)")
    public ResponseEntity<ApiResponse<TenantScreeningResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(screeningService.getById(id)));
    }

    // ─── GET /tenant-screenings/by-application/{applicationId} ────────────────

    @Operation(summary = "Obtener screening por rental application id")
    @GetMapping("/by-application/{applicationId}")
    @PreAuthorize("isAuthenticated() and @screeningSecurity.hasReadAccessByApplication(#applicationId, principal)")
    public ResponseEntity<ApiResponse<TenantScreeningResponse>> getByApplicationId(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(screeningService.getByApplicationId(applicationId)));
    }
}
