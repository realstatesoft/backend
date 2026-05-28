package com.openroof.openroof.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtro de rate-limiting para los endpoints de autenticación.
 *
 * <p>Aplica límites por IP a:
 * <ul>
 *   <li>{@code POST /auth/login} — límites de login por IP (el límite por email se aplica en {@link com.openroof.openroof.service.AuthService})</li>
 *   <li>{@code POST /auth/register} y {@code POST /auth/register-agent} — límite de registro por IP</li>
 *   <li>{@code POST /auth/refresh-token} — límite de refresh por IP</li>
 * </ul>
 *
 * <p>Devuelve HTTP 429 con cuerpo JSON cuando se supera el límite.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private final AuthRateLimiter authRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !"/auth/login".equals(path)
                && !"/auth/register".equals(path)
                && !"/auth/register-agent".equals(path)
                && !"/auth/refresh-token".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = extractClientIp(request);
        String path = request.getServletPath();

        boolean allowed = switch (path) {
            case "/auth/login"         -> authRateLimiter.isLoginAllowedForIp(ip);
            case "/auth/register",
                 "/auth/register-agent" -> authRateLimiter.isRegisterAllowedForIp(ip);
            case "/auth/refresh-token" -> authRateLimiter.isRefreshAllowedForIp(ip);
            default                    -> true;
        };

        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "success", false,
                    "message", "Demasiados intentos. Por favor, intente más tarde."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
