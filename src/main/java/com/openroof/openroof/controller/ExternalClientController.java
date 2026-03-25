package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.agent.CreateExternalClientRequest;
import com.openroof.openroof.dto.agent.ExternalClientResponse;
import com.openroof.openroof.dto.agent.UpdateExternalClientRequest;
import com.openroof.openroof.service.ExternalClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.openroof.openroof.model.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/external-clients", "/clients/external"})
@RequiredArgsConstructor
@Tag(name = "External Clients", description = "CRUD de clientes externos (prospectos)")
public class ExternalClientController {

    private final ExternalClientService externalClientService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear un nuevo cliente externo")
    public ResponseEntity<ApiResponse<ExternalClientResponse>> create(
            @Valid @RequestBody CreateExternalClientRequest request,
            @AuthenticationPrincipal User user) {
        ExternalClientResponse response = externalClientService.create(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Cliente externo creado exitosamente"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccessExternal(#id, authentication.principal)")
    @Operation(summary = "Obtener un cliente externo por ID")
    public ResponseEntity<ApiResponse<ExternalClientResponse>> getById(@PathVariable Long id) {
        ExternalClientResponse response = externalClientService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccessExternal(#id, authentication.principal)")
    @Operation(summary = "Actualizar un cliente externo")
    public ResponseEntity<ApiResponse<ExternalClientResponse>> update(@PathVariable Long id, @Valid @RequestBody UpdateExternalClientRequest request) {
        ExternalClientResponse response = externalClientService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cliente externo actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @agentClientSecurity.canAccessExternal(#id, authentication.principal)")
    @Operation(summary = "Eliminar un cliente externo")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        externalClientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
