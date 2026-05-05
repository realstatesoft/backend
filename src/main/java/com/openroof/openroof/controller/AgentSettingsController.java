package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.settings.AgentSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAgentSettingsRequest;
import com.openroof.openroof.service.AgentSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/settings/agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Agent Settings", description = "Configuración personal del agente")
public class AgentSettingsController {

    private final AgentSettingsService agentSettingsService;

    @GetMapping
    @Operation(summary = "Obtener configuración del agente autenticado")
    public ResponseEntity<ApiResponse<AgentSettingsResponse>> getSettings(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(agentSettingsService.getSettings(principal.getName())));
    }

    @PutMapping
    @Operation(summary = "Actualizar configuración del agente autenticado")
    public ResponseEntity<ApiResponse<AgentSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateAgentSettingsRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                agentSettingsService.updateSettings(principal.getName(), request),
                "Configuración actualizada"));
    }
}
