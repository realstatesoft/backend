package com.openroof.openroof.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.AgentAgendaResponse;
import com.openroof.openroof.dto.agent.CreateAgentAgendaRequest;
import com.openroof.openroof.dto.agent.UpdateAgentAgendaRequest;
import com.openroof.openroof.service.AgentAgendaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agent-agenda")
@RequiredArgsConstructor
@Tag(name = "Agent Agenda", description = "Gestión de la agenda del agente")
public class AgentAgendaController {

    private final AgentAgendaService agentAgendaService;

    // ─── CREATE ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Crear un nuevo evento en la agenda (solo AGENT)")
    public ResponseEntity<ApiResponse<AgentAgendaResponse>> create(
            @Valid @RequestBody CreateAgentAgendaRequest request,
            Principal principal) {

        AgentAgendaResponse response = agentAgendaService.create(request, principal.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Evento creado exitosamente"));
    }

    // ─── READ ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Obtener un evento por ID")
    public ResponseEntity<ApiResponse<AgentAgendaResponse>> getById(
            @Parameter(description = "ID del evento") @PathVariable Long id,
            Principal principal) {

        AgentAgendaResponse response = agentAgendaService.getById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Listar eventos del agente autenticado en un mes determinado")
    public ResponseEntity<ApiResponse<List<AgentAgendaResponse>>> getAgendaForMonth(
            @Parameter(description = "Año y mes en formato YYYY-MM") 
            @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            Principal principal) {
        
        LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = month.atEndOfMonth().atTime(java.time.LocalTime.MAX);

        List<AgentAgendaResponse> response = agentAgendaService.getAgendaForMonth(principal.getName(), startOfMonth, endOfMonth);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Actualizar un evento en la agenda")
    public ResponseEntity<ApiResponse<AgentAgendaResponse>> update(
            @Parameter(description = "ID del evento") @PathVariable Long id,
            @Valid @RequestBody UpdateAgentAgendaRequest request,
            Principal principal) {

        AgentAgendaResponse response = agentAgendaService.update(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Evento actualizado exitosamente"));
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Eliminar un evento de la agenda")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID del evento") @PathVariable Long id,
            Principal principal) {

        agentAgendaService.delete(id, principal.getName());
        return ResponseEntity.<ApiResponse<Void>>status(HttpStatus.NO_CONTENT).build();
    }
}
