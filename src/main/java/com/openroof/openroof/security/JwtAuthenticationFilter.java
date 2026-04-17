package com.openroof.openroof.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Filtro que intercepta cada request para validar el token JWT.
 * Si el usuario autenticado está suspendido (y no es ADMIN), corta la cadena
 * y devuelve 403 con un body JSON explicativo.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, userDetails)) {

                // ─── Bloqueo por suspensión ───────────────────────────
                if (userDetails instanceof User user && user.getRole() != UserRole.ADMIN
                        && userService.isUserSuspended(user)) {

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");

                    Map<String, String> body = Map.of(
                            "error", "ACCOUNT_SUSPENDED",
                            "message", "Tu cuenta ha sido suspendida. Contacta al administrador.",
                            "suspendedUntil", user.getSuspendedUntil().equals(LocalDateTime.MAX)
                                    ? "indefinido"
                                    : user.getSuspendedUntil().toString()
                    );
                    new ObjectMapper().writeValue(response.getOutputStream(), body);
                    return;
                }
                // ─────────────────────────────────────────────────────

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
