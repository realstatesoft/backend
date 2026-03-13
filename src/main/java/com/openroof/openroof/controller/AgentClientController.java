package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.service.AgentClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.parameters.P;

import com.openroof.openroof.model.user.User;

@RestController
@RequestMapping("/agent-clients")
@RequiredArgsConstructor
@Tag(name = "Agent Clients", description = "CRUD de clientes asociados a agentes")
public class AgentClientController {

    private final AgentClientService agentClientService;

    // ─── CREATE ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canManageAgent(#request.agentId, principal)")
    @Operation(summary = "Asociar un cliente a un agente")
    public ResponseEntity<ApiResponse<AgentClientResponse>> create(
            @P("request") @Valid @RequestBody CreateAgentClientRequest request) {

        AgentClientResponse response = agentClientService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Cliente de agente creado exitosamente"));
    }

    // ─── READ ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#id, principal)")
    @Operation(summary = "Obtener un cliente de agente por ID")
    public ResponseEntity<ApiResponse<AgentClientResponse>> getById(
            @P("id") @Parameter(description = "ID del registro agent-client") @PathVariable Long id) {

        AgentClientResponse response = agentClientService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canManageAgent(#agentId, principal)")
    @Operation(summary = "Listar los clientes de un agente (paginado)")
    public ResponseEntity<ApiResponse<Page<AgentClientSummaryResponse>>> getByAgent(
            @P("agentId") @Parameter(description = "ID del agente") @PathVariable Long agentId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AgentClientSummaryResponse> page = agentClientService.getByAgent(agentId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#id, principal)")
    @Operation(summary = "Actualizar un cliente de agente (parcial)")
    public ResponseEntity<ApiResponse<AgentClientResponse>> update(
            @P("id") @Parameter(description = "ID del registro agent-client") @PathVariable Long id,
            @Valid @RequestBody UpdateAgentClientRequest request) {

        AgentClientResponse response = agentClientService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cliente de agente actualizado exitosamente"));
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#id, principal)")
    @Operation(summary = "Eliminar un cliente de agente")
    public ResponseEntity<ApiResponse<Void>> delete(
            @P("id") @Parameter(description = "ID del registro agent-client") @PathVariable Long id) {

        agentClientService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
