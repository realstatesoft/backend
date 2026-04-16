package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.preference.*;
import com.openroof.openroof.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestionar las preferencias de búsqueda de los usuarios.
 */
@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "Gestión de preferencias de búsqueda y wizard de onboarding")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping("/options")
    @Operation(summary = "Obtener catálogo de opciones de preferencias (Público)")
    public ResponseEntity<ApiResponse<List<PreferenceCategoryDTO>>> getOptions() {
        List<PreferenceCategoryDTO> options = userPreferenceService.getPreferenceOptions();
        return ResponseEntity.ok(ApiResponse.ok(options));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener preferencias actuales de un usuario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserPreferenceResponseDTO>> getByUserId(@PathVariable Long userId) {
        UserPreferenceResponseDTO response = userPreferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @Operation(summary = "Guardar o actualizar preferencias (Upsert)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserPreferenceResponseDTO>> save(
            @Valid @RequestBody UserPreferenceRequestDTO request) {
        UserPreferenceResponseDTO response = userPreferenceService.saveOrUpdateUserPreferences(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Preferencias guardadas exitosamente"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Eliminar preferencias de un usuario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long userId) {
        userPreferenceService.deleteUserPreferences(userId);
        return ResponseEntity.noContent().build();
    }
}
