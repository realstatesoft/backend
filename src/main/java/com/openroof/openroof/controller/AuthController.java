package com.openroof.openroof.controller;

import java.security.Principal; // IMPORT CORRECTO
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.register.RegisterRequest;
import com.openroof.openroof.dto.register.AgentSignupRequest;
import com.openroof.openroof.dto.security.AuthResponse;
import com.openroof.openroof.dto.security.LoginRequest;
import com.openroof.openroof.service.AuthService; // PARA VALIDACIONES

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para registro, login y gestión de sesiones")
@CrossOrigin(originPatterns = "*")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Autentica credenciales y retorna tokens de acceso y refresco")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) { // Inyectamos el request
        return ResponseEntity.ok(ApiResponse.ok(authService.login(loginRequest, request), "Login exitoso"));
    }

    /*
     * * Auth: Enrique Rios
     * Desc: Registra un nuevo usuario y crea su sesión inicial.
     */
    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta y retorna tokens de acceso")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) { // Inyectamos el request
        return ResponseEntity.ok(ApiResponse.ok(authService.register(registerRequest, request), "Registro exitoso"));
    }

    /*
     * Desc: Endpoint específico para registrar agentes.
     * Utiliza la misma lógica que el registro normal pero forzando Role.AGENT
     * y permitiendo campos adicionales específicos de agente.
     */
    @Operation(
        summary = "Registrar agente", 
        description = "Endpoint específico para registro de agentes. Fuerza automáticamente el role AGENT y permite campos adicionales como empresa y licencia."
    )
    @PostMapping("/register-agent")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAgent(
            @Valid @RequestBody AgentSignupRequest agentSignupRequest,
            HttpServletRequest request) {
        return ResponseEntity.ok(
            ApiResponse.ok(
                authService.registerAgent(agentSignupRequest, request), 
                "Registro de agente exitoso"
            )
        );
    }

    /*
     * * Auth: Enrique Rios
     * Desc: Renueva el Access Token usando el Refresh Token.
     */
    @Operation(summary = "Refrescar token", description = "Genera un nuevo Access Token y rota el Refresh Token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {

        String refreshToken = requestBody.get("refreshToken");
        return ResponseEntity.ok(
                ApiResponse.ok(authService.refreshToken(refreshToken, request), "Token renovado exitosamente"));
    }

    /*
     * * Auth: Enrique Rios
     * Desc: Cierra la sesión actual (Invalida el Refresh Token en DB).
     */
    @Operation(summary = "Cerrar sesión", description = "Invalida el token actual del usuario en el servidor")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada exitosamente"));
    }

    /*
     * * Auth: Enrique Rios
     * Desc: Invalida TODAS las sesiones del usuario (Logout global).
     */
    @Operation(summary = "Cerrar todas las sesiones", description = "Expulsa al usuario de todos sus dispositivos")
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Principal principal) {
        // principal.getName() devuelve el email/username del token
        authService.logoutAllSessions(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Se han cerrado todas las sesiones exitosamente"));
    }
}
