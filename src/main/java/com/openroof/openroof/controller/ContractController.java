package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.contract.*;
import com.openroof.openroof.service.ContractService;
import com.openroof.openroof.service.ContractPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private final ContractService    contractService;
    private final ContractPdfService contractPdfService;


    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Crear un contrato",
               description = """
               Escenarios soportados:
               A) Directo propietario→usuario: sin agentes, commissionPct=0
               B) Un solo agente listador: listingAgentId presente, buyerAgentId null
               C) Un solo agente comprador: buyerAgentId presente, listingAgentId null
               D) Dual agency: ambos agentes, pcts suman commissionPct
               """)
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody ContractRequest request,
            Authentication auth) {

        ContractResponse response = contractService.create(request, auth.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Contrato creado exitosamente"));
    }

    // ─── UPDATE (Edit Draft) ──────────────────────────────────────────────────

    @PutMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Actualizar un borrador de contrato",
               description = "Permite editar un contrato siempre que su estado sea DRAFT.")
    public ResponseEntity<ApiResponse<ContractResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractRequest request,
            Authentication auth) {

        ContractResponse response = contractService.update(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Cambios guardados exitosamente"));
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
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Todos los contratos de una propiedad para el propietario o agentes relacionados")
    public ResponseEntity<ApiResponse<List<ContractSummaryResponse>>> getByProperty(
            @Parameter(description = "ID de la propiedad") @PathVariable Long propertyId,
            Authentication auth) {

        List<ContractSummaryResponse> list = contractService.getByProperty(propertyId, auth.getName());
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

    // ─── SIGNATURES ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/sign")
    @Operation(summary = "Firmar un contrato digitalmente",
               description = "Permite a una de las partes registrar su firma. Cambia el estado a PARTIALLY_SIGNED o SIGNED.")
    public ResponseEntity<ApiResponse<ContractResponse>> sign(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            @Valid @RequestBody SignContractRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            Authentication auth) {

        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String ip = (xForwardedFor != null && !xForwardedFor.isBlank())
                ? xForwardedFor.split(",")[0].trim()
                : httpRequest.getRemoteAddr();

        ContractResponse response = contractService.sign(id, request, auth.getName(), ip);
        return ResponseEntity.ok(ApiResponse.ok(response, "Firma registrada exitosamente"));
    }

    @GetMapping("/{id}/signatures")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'USER')")
    @Operation(summary = "Obtener el estado de firmas del contrato")
    public ResponseEntity<ApiResponse<List<SignatureStatusResponse>>> getSignatures(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            Authentication auth) {

        List<SignatureStatusResponse> list = contractService.getSignatures(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar (soft delete) un contrato (solo ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            Authentication auth) {

        contractService.delete(id, auth.getName());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.ok(null, "Contrato eliminado"));
    }

    // ─── PDF ──────────────────────────────────────────────────────────────────

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descargar el contrato en formato PDF",
               description = "Genera y devuelve el contrato como archivo PDF. Solo accesible para las partes involucradas o ADMIN.")
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "ID del contrato") @PathVariable Long id,
            Authentication auth) {

        byte[] pdfBytes = contractPdfService.generatePdf(id, auth.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "contrato-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
