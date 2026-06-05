package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.property.AssignPropertyRequest;
import com.openroof.openroof.dto.property.AssignmentStatusResponse;
import com.openroof.openroof.dto.property.PropertyAssignmentResponse;
import com.openroof.openroof.service.PropertyAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    // ─── ASSIGNMENT STATUS (owner) ───────────────────────────────

    @GetMapping("/properties/{propertyId}/assignment-status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener estado de asignación de una propiedad (propietario o ADMIN)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado de asignación (puede ser null si no hay asignación previa)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tienes permiso para ver esta propiedad"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Propiedad no encontrada")
    })
    public ResponseEntity<ApiResponse<AssignmentStatusResponse>> getAssignmentStatus(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            Principal principal) {

        AssignmentStatusResponse response = assignmentService.getAssignmentStatus(propertyId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ─── ASSIGN (owner solicita) ──────────────────────────────────

    @PostMapping("/properties/{propertyId}/assignments")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Solicitar asignación de agente a una propiedad (solo OWNER/ADMIN)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Solicitud de asignación creada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ya existe una asignación activa para este agente en esta propiedad"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Solo el propietario puede gestionar las asignaciones"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Propiedad o agente no encontrado")
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Asignación aceptada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Solo se puede responder a asignaciones PENDING"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Solo el agente asignado puede responder"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Asignación rechazada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Solo se puede responder a asignaciones PENDING"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Solo el agente asignado puede responder"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Asignación revocada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No se puede revocar una asignación en estado REVOKED o REJECTED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Solo el propietario puede revocar"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de asignaciones"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tienes permiso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Propiedad no encontrada")
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de asignaciones del agente (incluye PENDING y ACCEPTED)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tienes perfil de agente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Perfil de agente no encontrado")
    })
    public ResponseEntity<ApiResponse<List<PropertyAssignmentResponse>>> getMyAssignments(Principal principal) {

        List<PropertyAssignmentResponse> response = assignmentService.getMyAssignments(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
