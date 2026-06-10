package com.openroof.openroof.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OR-362 (SEC-004) — CORS en producción restringido a dominios exactos.
 *
 * Verifica que {@link SecurityConfig#corsConfigurationSource()}:
 *  - descarte cualquier origen con wildcard (p. ej. https://*.vercel.app),
 *  - mantenga solo los orígenes exactos configurados (via CORS_ALLOWED_ORIGINS / CORS_ALLOWED_PREVIEW_ORIGINS),
 *  - habilite credenciales solo para esos orígenes exactos.
 */
class SecurityConfigCorsTest {

    /** Construye la config de CORS inyectando los orígenes por reflexión (sin levantar Spring). */
    private CorsConfiguration buildCors(String allowedOrigins, String previewOrigins) {
        // corsConfigurationSource() no usa las dependencias del constructor → se pasan null.
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOriginsRaw", allowedOrigins);
        ReflectionTestUtils.setField(config, "allowedPreviewOriginsRaw", previewOrigins);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        return source.getCorsConfigurations().get("/**");
    }

    @Test
    void descarta_origenes_con_wildcard() {
        CorsConfiguration cors = buildCors("https://app.openroof.com,https://*.vercel.app", "");

        assertNotNull(cors.getAllowedOrigins());
        assertTrue(cors.getAllowedOrigins().contains("https://app.openroof.com"),
                "Debe conservar el dominio exacto");
        assertFalse(cors.getAllowedOrigins().stream().anyMatch(o -> o.contains("*")),
                "No debe quedar ningún origen con wildcard");
        assertFalse(cors.getAllowedOrigins().contains("https://*.vercel.app"),
                "El wildcard de Vercel debe ser ignorado");
    }

    @Test
    void permite_solo_dominios_exactos_y_con_credenciales() {
        CorsConfiguration cors = buildCors(
                "https://app.openroof.com,https://www.openroof.com",
                "https://openroof-qa.vercel.app"); // preview exacto (sin wildcard) → permitido

        assertEquals(
                List.of("https://app.openroof.com", "https://www.openroof.com", "https://openroof-qa.vercel.app"),
                cors.getAllowedOrigins());
        assertEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "Las credenciales se habilitan solo para orígenes exactos");
    }
}
