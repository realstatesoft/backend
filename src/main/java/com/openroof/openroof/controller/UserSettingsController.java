package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.settings.UpdateUserSettingsRequest;
import com.openroof.openroof.dto.settings.UserSettingsResponse;
import com.openroof.openroof.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/settings/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "User Settings", description = "Configuración personal del usuario")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    @Operation(summary = "Obtener configuración del usuario autenticado")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(userSettingsService.getSettings(principal.getName())));
    }

    @PutMapping
    @Operation(summary = "Actualizar configuración del usuario autenticado")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateUserSettingsRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                userSettingsService.updateSettings(principal.getName(), request),
                "Configuración actualizada"));
    }
}
