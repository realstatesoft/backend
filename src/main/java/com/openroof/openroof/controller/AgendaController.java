package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agenda.AgendaEventResponse;
import com.openroof.openroof.dto.agenda.CreateAgendaEventRequest;
import com.openroof.openroof.service.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Agenda", description = "Gestión de citas y eventos del agente")
public class AgendaController {

    private final AgendaService agendaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar todos los eventos del agente autenticado")
    public ResponseEntity<ApiResponse<List<AgendaEventResponse>>> getAll(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<AgendaEventResponse> events;
        if (from != null && to != null) {
            events = agendaService.getByDateRange(auth.getName(), from, to);
        } else {
            events = agendaService.getAll(auth.getName());
        }
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Próximos eventos del agente")
    public ResponseEntity<ApiResponse<List<AgendaEventResponse>>> getUpcoming(
            Authentication auth,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                agendaService.getUpcoming(auth.getName(), limit)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear nuevo evento en la agenda")
    public ResponseEntity<ApiResponse<AgendaEventResponse>> create(
            Authentication auth,
            @Valid @RequestBody CreateAgendaEventRequest request) {
        AgendaEventResponse response = agendaService.create(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Evento creado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar un evento de la agenda")
    public ResponseEntity<Void> delete(
            Authentication auth,
            @PathVariable Long id) {
        agendaService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
