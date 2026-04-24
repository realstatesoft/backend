package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.contract.*;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.service.ContractTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contract-templates")
@RequiredArgsConstructor
@Tag(name = "Contract templates", description = "Plantillas reutilizables para contratos (ADMIN y lectura activa para agentes)")
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear plantilla")
    public ResponseEntity<ApiResponse<ContractTemplateResponse>> create(
            @Valid @RequestBody ContractTemplateCreateRequest request) {

        ContractTemplateResponse created = contractTemplateService.createTemplate(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Plantilla creada"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar plantilla")
    public ResponseEntity<ApiResponse<ContractTemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractTemplateUpdateRequest request) {

        ContractTemplateResponse updated = contractTemplateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Plantilla actualizada"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar plantillas (filtros opcionales)")
    public ResponseEntity<ApiResponse<List<ContractTemplateSummaryResponse>>> list(
            @Parameter(description = "Filtrar por tipo de contrato")
            @RequestParam(required = false) ContractType contractType,
            @Parameter(description = "Filtrar por activo (true/false)")
            @RequestParam(required = false) Boolean active) {

        List<ContractTemplateSummaryResponse> list = contractTemplateService.getTemplates(contractType, active);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Plantillas activas por tipo (para alta de contratos)")
    public ResponseEntity<ApiResponse<List<ContractTemplateResponse>>> listActive(
            @RequestParam ContractType contractType) {

        List<ContractTemplateResponse> list = contractTemplateService.getActiveTemplates(contractType);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener plantilla por ID")
    public ResponseEntity<ApiResponse<ContractTemplateResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(contractTemplateService.getById(id)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar plantilla")
    public ResponseEntity<ApiResponse<ContractTemplateResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(contractTemplateService.activateTemplate(id), "Plantilla activada"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar plantilla")
    public ResponseEntity<ApiResponse<ContractTemplateResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(contractTemplateService.deactivateTemplate(id), "Plantilla desactivada"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar plantilla (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        contractTemplateService.softDeleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Plantilla eliminada"));
    }
}
