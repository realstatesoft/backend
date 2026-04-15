package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.preference.PreferenceCategoryDTO;
import com.openroof.openroof.dto.preference.UserPreferenceRequestDTO;
import com.openroof.openroof.dto.preference.UserPreferenceResponseDTO;
import com.openroof.openroof.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para el módulo de preferencias de usuario.
 * Base path: /api/v1/preferences (el prefijo /api lo agrega el context-path del servidor)
 */
@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "Preferencias de búsqueda del usuario para el onboarding wizard")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * GET /api/v1/preferences/options
     * Retorna todas las categorías con sus opciones disponibles.
     * Endpoint público — no requiere autenticación.
     */
    @GetMapping("/options")
    @Operation(summary = "Obtener catálogo completo de opciones de preferencia",
               description = "Retorna todas las categorías con sus opciones. No requiere autenticación.")
    public ResponseEntity<ApiResponse<List<PreferenceCategoryDTO>>> getPreferenceOptions() {
        List<PreferenceCategoryDTO> categories = userPreferenceService.getPreferenceOptions();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    /**
     * GET /api/v1/preferences/{userId}
     * Retorna las preferencias guardadas del usuario.
     * Si no existen, devuelve 200 con onboardingCompleted=false y listas vacías.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Obtener preferencias de un usuario",
               description = "Retorna las preferencias guardadas. Si no existen, devuelve onboardingCompleted=false con listas vacías.")
    public ResponseEntity<ApiResponse<UserPreferenceResponseDTO>> getUserPreferences(
            @Parameter(description = "ID del usuario") @PathVariable Long userId) {
        UserPreferenceResponseDTO response = userPreferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * POST /api/v1/preferences
     * Crea o actualiza las preferencias del usuario (upsert completo).
     */
    @PostMapping
    @PreAuthorize("#request.userId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Guardar o actualizar preferencias de usuario",
               description = "Crea o reemplaza completamente las preferencias del usuario. Marca onboardingCompleted=true.")
    public ResponseEntity<ApiResponse<UserPreferenceResponseDTO>> saveOrUpdatePreferences(
            @Valid @RequestBody UserPreferenceRequestDTO request) {
        UserPreferenceResponseDTO response = userPreferenceService.saveOrUpdateUserPreferences(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Preferencias guardadas exitosamente"));
    }

    /**
     * DELETE /api/v1/preferences/{userId}
     * Elimina todas las preferencias del usuario.
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Eliminar preferencias de un usuario")
    public ResponseEntity<Void> deletePreferences(
            @Parameter(description = "ID del usuario") @PathVariable Long userId) {
        userPreferenceService.deleteUserPreferences(userId);
        return ResponseEntity.noContent().build();
    }
}
