package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.user.SuspendUserRequest;
import com.openroof.openroof.dto.user.UpdateUserRequest;
import com.openroof.openroof.dto.user.UserProfileResponse;
import com.openroof.openroof.dto.user.UserSearchResponse;
import com.openroof.openroof.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión del perfil del usuario autenticado")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Obtener perfil", description = "Retorna los datos personales del usuario autenticado")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Principal principal) {
        UserProfileResponse profile = userService.getProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @Operation(summary = "Editar datos personales", description = "Actualiza nombre, teléfono y/o avatar del usuario autenticado")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Principal principal) {

        UserProfileResponse updated = userService.updatePersonalData(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Datos personales actualizados exitosamente"));
    }

    @Operation(summary = "Buscar usuario por email",
               description = "Busca un usuario registrado por email exacto. Solo para agentes y admins.")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserSearchResponse>> searchByEmail(
            @RequestParam @NotBlank String email) {

        return userService.searchByEmail(email.trim())
                .map(result -> ResponseEntity.ok(ApiResponse.ok(result)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No se encontró un usuario con ese email")));
    }
    // ─── Suspensión (solo ADMIN) ──────────────────────────────────────────────

    @Operation(summary = "Suspender usuario", description = "Suspende la cuenta de un usuario. Solo ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario suspendido exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No se puede suspender a un administrador"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @Parameter(description = "ID del usuario a suspender") @PathVariable Long id,
            @Valid @RequestBody SuspendUserRequest request) {

        userService.suspendUser(id, request.suspendedUntil(), request.suspensionReason());
        return ResponseEntity.ok(ApiResponse.ok(null, "Usuario suspendido exitosamente"));
    }

    @Operation(summary = "Levantar suspensión", description = "Levanta la suspensión de un usuario. Solo ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suspensión levantada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}/unsuspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(
            @Parameter(description = "ID del usuario a levantar suspensión") @PathVariable Long id) {

        userService.unsuspendUser(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Suspensión levantada exitosamente"));
    }
}
