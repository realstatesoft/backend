package com.openroof.openroof.controller;

import com.openroof.openroof.model.user.User;
import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.service.AgentClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.parameters.P;

@RestController
@RequestMapping({"/agent-clients", "/clients"})
@RequiredArgsConstructor
@Tag(name = "Agent Clients", description = "CRUD de clientes asociados a agentes")
public class AgentClientController {

    private final AgentClientService agentClientService;

    // ─── CREATE ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canManageAgent(#request.agentId, authentication.principal)")
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
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#id, authentication.principal)")
    @Operation(summary = "Obtener un cliente de agente por ID")
    public ResponseEntity<ApiResponse<AgentClientResponse>> getById(
            @P("id") @Parameter(description = "ID del registro agent-client") @PathVariable Long id) {

        AgentClientResponse response = agentClientService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canManageAgent(#agentId, authentication.principal)")
    @Operation(summary = "Listar los clientes de un agente (paginado)")
    public ResponseEntity<ApiResponse<Page<AgentClientSummaryResponse>>> getByAgent(
            @P("agentId") @Parameter(description = "ID del agente") @PathVariable Long agentId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        PageRequest clampedPageable = PageRequest.of(
                pageable.getPageNumber(), size, pageable.getSort());

        Page<AgentClientSummaryResponse> page = agentClientService.getByAgent(agentId, clampedPageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.isAgent(authentication.principal)")
    @Operation(summary = "Buscar clientes del agente autenticado (paginado)")
    public ResponseEntity<ApiResponse<Page<AgentClientSummaryResponse>>> search(
            AgentClientSearchRequest criteria,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {

        // Extraer Agent ID del usuario autenticado. 
        // Asumimos que el usuario tiene un perfil de agente asociado.
        // Si no, el servicio fallará o devolverá vacío. 
        // Nota: El plan pide sacar el ID internamente.
        Long agentId = getAgentIdFromUser(currentUser);

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        PageRequest clampedPageable = PageRequest.of(
                pageable.getPageNumber(), size, pageable.getSort());

        Page<AgentClientSummaryResponse> page = agentClientService.searchClients(agentId, criteria, clampedPageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.isAgent(authentication.principal)")
    @Operation(summary = "Exportar clientes del agente autenticado a CSV")
    public ResponseEntity<byte[]> export(
            AgentClientSearchRequest criteria,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {

        Long agentId = getAgentIdFromUser(currentUser);
        String csv = agentClientService.exportClientsToCsv(agentId, criteria);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clients.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Long getAgentIdFromUser(User user) {
        // Esta lógica depende de cómo esté mapeado el AgentProfile al User.
        // Mirando AgentProfileController u otros services para referencia.
        // Si no está directamente en User, hay que buscarlo por user_id.
        // Por ahora lanzamos error si no se encuentra o lo resolvemos en el service.
        // Pero el plan dice "sacar el ID de Agente internamente a través del Principal".
        // Lo resolvemos pidiendo al service que lo busque por user.id si es necesario.
        // Pero para ser consistentes con el plan, lo pasamos al service como agentId.
        // Para simplificar, asumiremos que existe un método en un service que lo resuelve.
        // Implementación rápida para este caso:
        return agentClientService.getAgentIdByUser(user.getId());
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#id, authentication.principal)")
    @Operation(summary = "Actualizar un cliente de agente (parcial)")
    public ResponseEntity<ApiResponse<AgentClientResponse>> update(
            @P("id") @Parameter(description = "ID del registro agent-client") @PathVariable Long id,
            @Valid @RequestBody UpdateAgentClientRequest request) {

        AgentClientResponse response = agentClientService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cliente de agente actualizado exitosamente"));
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#id, authentication.principal)")
    @Operation(summary = "Eliminar un cliente de agente")
    public ResponseEntity<Void> delete(
            @P("id") @Parameter(description = "ID del registro agent-client") @PathVariable Long id) {

        agentClientService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
