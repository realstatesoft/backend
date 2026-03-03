package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.user.UpdateUserRequest;
import com.openroof.openroof.dto.user.UserProfileResponse;
import com.openroof.openroof.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Principal principal) {
        UserProfileResponse profile = userService.getProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @Operation(summary = "Editar datos personales", description = "Actualiza nombre, teléfono y/o avatar del usuario autenticado")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Principal principal) {

        UserProfileResponse updated = userService.updatePersonalData(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Datos personales actualizados exitosamente"));
    }
}
