package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.contract.*;
import com.openroof.openroof.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
@Tag(name = "Contracts", description = "Gestión de contratos de compraventa y alquiler con modelo de comisiones")
public class ContractController {

    private final ContractService contractService;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Crear un contrato (solo AGENT o ADMIN)",
               description = """
               Escenarios soportados:
               A) Directo propietario→usuario: sin agentes, commissionPct=0
               B) Un solo agente listador: listingAgentId presente, buyerAgentId null
               C) Un solo agente comprador: buyerAgentId presente, listingAgentId null
               D) Dual agency: ambos agentes, pcts suman commissionPct
               """)
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody ContractRequest request) {

        ContractResponse response = contractService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Contrato creado exitosamente"));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un contrato por ID",
               description = "Solo pueden acceder las partes involucradas o ADMIN")
    public ResponseEntity<ApiResponse<ContractResponse>> getById(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            Authentication auth) {

        ContractResponse response = contractService.getById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/as-listing-agent")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Contratos donde el agente autenticado actúa como agente listador")
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getAsListingAgent(
            Authentication auth) {

        List<ContractSummaryResponse> list = contractService.getAsListingAgent(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/as-buyer-agent")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Contratos donde el agente autenticado actúa como agente del comprador")
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getAsBuyerAgent(
            Authentication auth) {

        List<ContractSummaryResponse> list = contractService.getAsBuyerAgent(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/as-seller")
    @Operation(summary = "Contratos donde el usuario autenticado es vendedor/propietario")
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getAsSeller(
            Authentication auth) {

        List<ContractSummaryResponse> list = contractService.getAsSeller(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/as-buyer")
    @Operation(summary = "Contratos donde el usuario autenticado es comprador/inquilino")
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getAsBuyer(
            Authentication auth) {

        List<ContractSummaryResponse> list = contractService.getAsBuyer(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Todos los contratos de una propiedad (AGENT o ADMIN)")
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getByProperty(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId) {

        List<ContractSummaryResponse> list = contractService.getByProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Actualizar el estado de un contrato",
               description = """
               Transiciones válidas para AGENT:
                 DRAFT → SENT | CANCELLED
                 SENT → PARTIALLY_SIGNED | REJECTED | CANCELLED
                 PARTIALLY_SIGNED → SIGNED | REJECTED | CANCELLED
               ADMIN puede hacer cualquier transición.
               """)
    public ResponseEntity<ApiResponse<ContractResponse>> updateStatus(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            @Valid @RequestBody ContractStatusUpdateRequest request,
            Authentication auth) {

        ContractResponse response = contractService.updateStatus(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Estado del contrato actualizado"));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar (soft delete) un contrato (solo ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID del contrato") @PathVariable Long id) {

        contractService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.ok(null, "Contrato eliminado"));
    }
}
