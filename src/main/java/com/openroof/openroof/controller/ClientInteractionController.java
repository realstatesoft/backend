package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.ClientInteractionResponse;
import com.openroof.openroof.dto.agent.CreateClientInteractionRequest;
import com.openroof.openroof.dto.agent.UpdateClientInteractionRequest;
import com.openroof.openroof.model.enums.InteractionType;
import com.openroof.openroof.service.ClientInteractionService;
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
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients/{clientId}/interactions")
@RequiredArgsConstructor
@Tag(name = "Client Interactions", description = "Timeline y CRUD de interacciones CRM del cliente")
public class ClientInteractionController {

    private final ClientInteractionService clientInteractionService;

    @PostMapping
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#clientId, authentication.principal)")
    @Operation(summary = "Crear una interacción de cliente")
    public ResponseEntity<ApiResponse<ClientInteractionResponse>> create(
            @P("clientId") @Parameter(description = "ID del cliente CRM") @PathVariable Long clientId,
            @Valid @RequestBody CreateClientInteractionRequest request) {

        ClientInteractionResponse response = clientInteractionService.create(clientId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Interacción creada exitosamente"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#clientId, authentication.principal)")
    @Operation(summary = "Listar el timeline de interacciones del cliente")
    public ResponseEntity<ApiResponse<Page<ClientInteractionResponse>>> list(
            @P("clientId") @Parameter(description = "ID del cliente CRM") @PathVariable Long clientId,
            @RequestParam(required = false) InteractionType type,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ClientInteractionResponse> page = clientInteractionService.list(clientId, type, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PutMapping("/{interactionId}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#clientId, authentication.principal)")
    @Operation(summary = "Actualizar una interacción de cliente")
    public ResponseEntity<ApiResponse<ClientInteractionResponse>> update(
            @P("clientId") @Parameter(description = "ID del cliente CRM") @PathVariable Long clientId,
            @Parameter(description = "ID de la interacción") @PathVariable Long interactionId,
            @Valid @RequestBody UpdateClientInteractionRequest request) {

        ClientInteractionResponse response = clientInteractionService.update(clientId, interactionId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Interacción actualizada exitosamente"));
    }

    @DeleteMapping("/{interactionId}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccess(#clientId, authentication.principal)")
    @Operation(summary = "Eliminar una interacción de cliente")
    public ResponseEntity<Void> delete(
            @P("clientId") @Parameter(description = "ID del cliente CRM") @PathVariable Long clientId,
            @Parameter(description = "ID de la interacción") @PathVariable Long interactionId) {

        clientInteractionService.delete(clientId, interactionId);
        return ResponseEntity.noContent().build();
    }
}
