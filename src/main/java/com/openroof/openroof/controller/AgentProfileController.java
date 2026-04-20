package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.*;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.service.AgentProfileService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Tag(name = "Agents", description = "CRUD de perfiles de agentes inmobiliarios")
public class AgentProfileController {

    private final AgentProfileService agentProfileService;

    // ─── CREATE ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un nuevo perfil de agente (solo ADMIN)")
    public ResponseEntity<ApiResponse<AgentProfileResponse>> create(
            @Valid @RequestBody CreateAgentProfileRequest request) {

        AgentProfileResponse response = agentProfileService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Perfil de agente creado exitosamente"));
    }

    // ─── READ ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un perfil de agente por ID")
    public ResponseEntity<ApiResponse<AgentProfileResponse>> getById(
            @Parameter(description = "ID del agente") @PathVariable Long id) {

        AgentProfileResponse response = agentProfileService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Listar todos los agentes (paginado)")
    public ResponseEntity<ApiResponse<Page<AgentProfileSummaryResponse>>> getAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AgentProfileSummaryResponse> page = agentProfileService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar agentes por texto, especialidad y puntuación mínima")
    public ResponseEntity<ApiResponse<Page<AgentProfileSummaryResponse>>> search(
            @RequestParam(name = "q",         required = false) String keyword,
            @RequestParam(name = "specialty",  required = false) String specialty,
            @RequestParam(name = "minRating",  required = false) java.math.BigDecimal minRating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AgentProfileSummaryResponse> page =
                agentProfileService.searchWithFilters(keyword, specialty, minRating, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/suggested")
    @Operation(summary = "Obtener agentes sugeridos basados en tipo de propiedad y categoría")
    public ResponseEntity<ApiResponse<List<AgentProfileSummaryResponse>>> getSuggestedAgents(
            @Parameter(description = "Tipo de propiedad (HOUSE, APARTMENT, LAND, OFFICE, WAREHOUSE, FARM)")
            @RequestParam(required = false) PropertyType propertyType,

            @Parameter(description = "Categoría (SALE, RENT, SALE_OR_RENT)")
            @RequestParam(required = false) PropertyCategory category,

            @Parameter(description = "Cantidad máxima de agentes (default: 5, max: 20)")
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        SuggestedAgentsRequest request = new SuggestedAgentsRequest(propertyType, category, limit);

        List<AgentProfileSummaryResponse> agents = agentProfileService.getSuggestedAgents(request);
        return ResponseEntity.ok(ApiResponse.ok(agents));
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @leadSecurity.isAgentOwner(principal, #id)")
    @Operation(summary = "Actualizar un perfil de agente (ADMIN o el propio agente)")
    public ResponseEntity<ApiResponse<AgentProfileResponse>> update(
            @Parameter(description = "ID del agente") @PathVariable Long id,
            @Valid @RequestBody UpdateAgentProfileRequest request) {

        AgentProfileResponse response = agentProfileService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Perfil de agente actualizado exitosamente"));
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un perfil de agente (soft delete, solo ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID del agente") @PathVariable Long id) {

        agentProfileService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
