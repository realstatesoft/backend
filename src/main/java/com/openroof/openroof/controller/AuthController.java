package com.openroof.openroof.controller;

import java.security.Principal;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.register.RegisterRequest;
import com.openroof.openroof.dto.register.AgentSignupRequest;
import com.openroof.openroof.dto.security.AuthResponse;
import com.openroof.openroof.dto.security.LoginRequest;
import com.openroof.openroof.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para registro, login y gestión de sesiones")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final long REFRESH_COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60L;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Autentica credenciales y retorna token de acceso; emite refresh token como cookie HttpOnly")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(loginRequest, request);
        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login exitoso"));
    }

    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta y retorna token de acceso; emite refresh token como cookie HttpOnly")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(registerRequest, request);
        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Registro exitoso"));
    }

    @Operation(
        summary = "Registrar agente",
        description = "Registro de agentes con role AGENT forzado; emite refresh token como cookie HttpOnly"
    )
    @PostMapping("/register-agent")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAgent(
            @Valid @RequestBody AgentSignupRequest agentSignupRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.registerAgent(agentSignupRequest, request);
        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Registro de agente exitoso"));
    }

    @Operation(summary = "Refrescar token", description = "Lee el refresh token desde la cookie HttpOnly, genera nuevo access token y rota el refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenFromCookie,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshToken(refreshTokenFromCookie, request);
        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Token renovado exitosamente"));
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida el refresh token en servidor y borra la cookie HttpOnly")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenFromCookie,
            HttpServletResponse response) {
        authService.logout(refreshTokenFromCookie);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada exitosamente"));
    }

    @Operation(summary = "Cerrar todas las sesiones", description = "Expulsa al usuario de todos sus dispositivos y borra la cookie HttpOnly")
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            Principal principal,
            HttpServletResponse response) {
        authService.logoutAllSessions(principal.getName());
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Se han cerrado todas las sesiones exitosamente"));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(REFRESH_COOKIE_MAX_AGE_SECONDS))
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
