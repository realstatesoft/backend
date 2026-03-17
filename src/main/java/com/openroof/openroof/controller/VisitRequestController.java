package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.visit.CounterProposeRequest;
import com.openroof.openroof.dto.visit.CreateVisitRequestRequest;
import com.openroof.openroof.dto.visit.VisitRequestResponse;
import com.openroof.openroof.service.VisitRequestService;
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
@RequestMapping("/visit-requests")
@RequiredArgsConstructor
@Tag(name = "Visit Requests", description = "Solicitudes de visita y negociación de horario entre buyer y agent")
public class VisitRequestController {

    private final VisitRequestService visitRequestService;

    // ─── CREATE (buyer) ───────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Crear solicitud de visita (USER)")
    public ResponseEntity<ApiResponse<VisitRequestResponse>> create(
            @Valid @RequestBody CreateVisitRequestRequest request,
            Principal principal) {

        VisitRequestResponse response = visitRequestService.create(request, principal.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Solicitud de visita creada"));
    }

    // ─── ACCEPT (agent) ───────────────────────────────────────────

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Aceptar solicitud y confirmar visita (AGENT)")
    public ResponseEntity<ApiResponse<VisitRequestResponse>> accept(
            @Parameter(description = "ID de la solicitud") @PathVariable Long id,
            Principal principal) {

        VisitRequestResponse response = visitRequestService.accept(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Solicitud aceptada. Visita confirmada"));
    }

    // ─── REJECT (agent) ───────────────────────────────────────────

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Rechazar solicitud de visita (AGENT)")
    public ResponseEntity<ApiResponse<VisitRequestResponse>> reject(
            @Parameter(description = "ID de la solicitud") @PathVariable Long id,
            Principal principal) {

        VisitRequestResponse response = visitRequestService.reject(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Solicitud rechazada"));
    }

    // ─── COUNTER PROPOSE (agent) ──────────────────────────────────

    @PutMapping("/{id}/counter-propose")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Proponer nuevo horario (AGENT)")
    public ResponseEntity<ApiResponse<VisitRequestResponse>> counterPropose(
            @Parameter(description = "ID de la solicitud") @PathVariable Long id,
            @Valid @RequestBody CounterProposeRequest request,
            Principal principal) {

        VisitRequestResponse response = visitRequestService.counterPropose(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Nuevo horario propuesto"));
    }

    // ─── CANCEL (buyer) ───────────────────────────────────────────

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Cancelar solicitud de visita (USER)")
    public ResponseEntity<ApiResponse<VisitRequestResponse>> cancel(
            @Parameter(description = "ID de la solicitud") @PathVariable Long id,
            Principal principal) {

        VisitRequestResponse response = visitRequestService.cancel(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Solicitud cancelada"));
    }

    // ─── QUERIES ──────────────────────────────────────────────────

    @GetMapping("/me/buyer")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Mis solicitudes como comprador")
    public ResponseEntity<ApiResponse<List<VisitRequestResponse>>> getMyRequestsAsBuyer(Principal principal) {
        List<VisitRequestResponse> response = visitRequestService.getMyRequestsAsBuyer(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/agent")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Solicitudes asignadas a mí como agente")
    public ResponseEntity<ApiResponse<List<VisitRequestResponse>>> getMyRequestsAsAgent(Principal principal) {
        List<VisitRequestResponse> response = visitRequestService.getMyRequestsAsAgent(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Solicitudes de una propiedad (owner/agent/admin)")
    public ResponseEntity<ApiResponse<List<VisitRequestResponse>>> getByProperty(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            Principal principal) {

        List<VisitRequestResponse> response = visitRequestService.getByProperty(propertyId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
