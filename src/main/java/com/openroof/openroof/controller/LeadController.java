package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.lead.CreateLeadFromWizardRequest;
import com.openroof.openroof.dto.lead.CreateLeadInteractionRequest;
import com.openroof.openroof.dto.lead.LeadInteractionResponse;
import com.openroof.openroof.dto.lead.LeadResponse;
import com.openroof.openroof.dto.lead.UpdateLeadStatusRequest;
import com.openroof.openroof.service.LeadServiceApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadServiceApi leadService;

    /**
     * Crea un Lead desde el Sell Wizard.
     * Este endpoint es público para permitir a usuarios no autenticados enviar solicitudes.
     */
    @PostMapping("/wizard")
    public ResponseEntity<ApiResponse<LeadResponse>> createFromWizard(
            @Valid @RequestBody CreateLeadFromWizardRequest request) {
        LeadResponse response = leadService.createFromWizard(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Solicitud de contacto enviada exitosamente"));
    }

    /**
     * Obtiene los leads de un agente específico.
     * Solo el propio agente o un ADMIN puede ver los leads.
     */
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @leadSecurity.isAgentOwner(principal, #agentId))")
    public ResponseEntity<ApiResponse<Page<LeadResponse>>> getLeadsByAgent(
            @PathVariable Long agentId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<LeadResponse> leads = leadService.getLeadsByAgent(agentId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(leads));
    }

    /**
     * Obtiene un lead por ID.
     * Solo el agente propietario del lead o un ADMIN puede acceder.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @leadSecurity.isLeadOwner(principal, #id))")
    public ResponseEntity<ApiResponse<LeadResponse>> getById(@PathVariable Long id) {
        LeadResponse response = leadService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Actualiza el estado del pipeline del lead y registra interacción STATUS_CHANGE si cambia.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @leadSecurity.isLeadOwner(principal, #id))")
    public ResponseEntity<ApiResponse<LeadResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeadStatusRequest request) {
        LeadResponse response = leadService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Registra una interacción manual (nota, llamada, correo…).
     */
    @PostMapping("/{id}/interactions")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @leadSecurity.isLeadOwner(principal, #id))")
    public ResponseEntity<ApiResponse<LeadInteractionResponse>> addInteraction(
            @PathVariable Long id,
            @Valid @RequestBody CreateLeadInteractionRequest request) {
        LeadInteractionResponse response = leadService.addInteraction(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * Cuenta los leads de un agente.
     * Solo el propio agente o un ADMIN puede contar sus leads.
     */
    @GetMapping("/agent/{agentId}/count")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @leadSecurity.isAgentOwner(principal, #agentId))")
    public ResponseEntity<ApiResponse<Long>> countByAgent(@PathVariable Long agentId) {
        long count = leadService.countByAgent(agentId);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }
}
