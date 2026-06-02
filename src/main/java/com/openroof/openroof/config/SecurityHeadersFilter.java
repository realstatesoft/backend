package com.openroof.openroof.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers for API responses.
 * CSP for the SPA is enforced at the CDN (Vercel) — see frontend/vercel.json.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String API_CSP_POLICY =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    @Value("${security.headers.csp-enabled:false}")
    private boolean cspEnabled;

    @Value("${security.headers.csp-api-policy:}")
    private String cspApiPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()");
        response.setHeader("X-Content-Type-Options", "nosniff");

        if (cspEnabled) {
            String policy = (cspApiPolicy == null || cspApiPolicy.isBlank()) ? API_CSP_POLICY : cspApiPolicy.trim();
            response.setHeader("Content-Security-Policy", policy);
        }

        filterChain.doFilter(request, response);
    }
}
