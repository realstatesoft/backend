package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.AssignPropertyRequest;
import com.openroof.openroof.dto.property.PropertyAssignmentResponse;
import com.openroof.openroof.service.PropertyAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Property Assignments", description = "Delegación de gestión de propiedades a agentes")
public class PropertyAssignmentController {

    private final PropertyAssignmentService assignmentService;

    // ─── ASSIGN (owner solicita) ──────────────────────────────────

    @PostMapping("/properties/{propertyId}/assignments")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Solicitar asignación de agente a una propiedad (solo OWNER/ADMIN)")
    public ResponseEntity<ApiResponse<PropertyAssignmentResponse>> assign(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            @Valid @RequestBody AssignPropertyRequest request,
            Principal principal) {

        PropertyAssignmentResponse response = assignmentService.assign(propertyId, request, principal.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Solicitud de asignación enviada al agente"));
    }

    // ─── ACCEPT (agent acepta) ────────────────────────────────────

    @PutMapping("/assignments/{assignmentId}/accept")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Aceptar una asignación pendiente (solo AGENT)")
    public ResponseEntity<ApiResponse<PropertyAssignmentResponse>> accept(
            @Parameter(description = "ID de la asignación") @PathVariable Long assignmentId,
            Principal principal) {

        PropertyAssignmentResponse response = assignmentService.accept(assignmentId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Asignación aceptada"));
    }

    // ─── REJECT (agent rechaza) ───────────────────────────────────

    @PutMapping("/assignments/{assignmentId}/reject")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Rechazar una asignación pendiente (solo AGENT)")
    public ResponseEntity<ApiResponse<PropertyAssignmentResponse>> reject(
            @Parameter(description = "ID de la asignación") @PathVariable Long assignmentId,
            Principal principal) {

        PropertyAssignmentResponse response = assignmentService.reject(assignmentId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Asignación rechazada"));
    }

    // ─── REVOKE (owner revoca) ────────────────────────────────────

    @PutMapping("/assignments/{assignmentId}/revoke")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Revocar una asignación (solo OWNER/ADMIN)")
    public ResponseEntity<ApiResponse<PropertyAssignmentResponse>> revoke(
            @Parameter(description = "ID de la asignación") @PathVariable Long assignmentId,
            Principal principal) {

        PropertyAssignmentResponse response = assignmentService.revoke(assignmentId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Asignación revocada"));
    }

    // ─── LIST by property (owner/admin) ──────────────────────────

    @GetMapping("/properties/{propertyId}/assignments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar asignaciones de una propiedad (propietario o ADMIN)")
    public ResponseEntity<ApiResponse<List<PropertyAssignmentResponse>>> getByProperty(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            Principal principal) {

        List<PropertyAssignmentResponse> response = assignmentService.getByProperty(propertyId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ─── MY assignments (agent) ───────────────────────────────────

    @GetMapping("/assignments/me")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Listar mis asignaciones como agente (solo AGENT)")
    public ResponseEntity<ApiResponse<List<PropertyAssignmentResponse>>> getMyAssignments(Principal principal) {

        List<PropertyAssignmentResponse> response = assignmentService.getMyAssignments(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
