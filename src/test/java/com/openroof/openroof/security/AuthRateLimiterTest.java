package com.openroof.openroof.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {

    private AuthRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new AuthRateLimiter();
        // Límites pequeños para que los tests sean rápidos
        ReflectionTestUtils.setField(rateLimiter, "loginMaxPerIp",      3);
        ReflectionTestUtils.setField(rateLimiter, "loginMaxPerEmail",   2);
        ReflectionTestUtils.setField(rateLimiter, "loginWindowMinutes", 15);
        ReflectionTestUtils.setField(rateLimiter, "registerMaxPerIp",      2);
        ReflectionTestUtils.setField(rateLimiter, "registerWindowMinutes", 60);
        ReflectionTestUtils.setField(rateLimiter, "refreshMaxPerIp",      4);
        ReflectionTestUtils.setField(rateLimiter, "refreshWindowMinutes", 15);
    }

    // ── Login por IP ────────────────────────────────────────────────────────

    @Test
    void loginByIp_allowsUpToLimit() {
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.isLoginAllowedForIp("1.2.3.4"),
                    "Intento " + (i + 1) + " debería ser permitido");
        }
    }

    @Test
    void loginByIp_blocksWhenLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.isLoginAllowedForIp("1.2.3.4");
        }
        assertFalse(rateLimiter.isLoginAllowedForIp("1.2.3.4"),
                "El 4to intento debería ser bloqueado");
    }

    @Test
    void loginByIp_differentIpsAreIndependent() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.isLoginAllowedForIp("1.2.3.4");
        }
        assertTrue(rateLimiter.isLoginAllowedForIp("9.9.9.9"),
                "Una IP diferente no debe verse afectada");
    }

    // ── Login por email ─────────────────────────────────────────────────────

    @Test
    void loginByEmail_allowsUpToLimit() {
        assertTrue(rateLimiter.isLoginAllowedForEmail("user@test.com"));
        assertTrue(rateLimiter.isLoginAllowedForEmail("user@test.com"));
    }

    @Test
    void loginByEmail_blocksWhenLimitExceeded() {
        rateLimiter.isLoginAllowedForEmail("user@test.com");
        rateLimiter.isLoginAllowedForEmail("user@test.com");
        assertFalse(rateLimiter.isLoginAllowedForEmail("user@test.com"),
                "El 3er intento para el mismo email debería ser bloqueado");
    }

    @Test
    void loginByEmail_isCaseInsensitive() {
        rateLimiter.isLoginAllowedForEmail("User@Test.COM");
        rateLimiter.isLoginAllowedForEmail("USER@TEST.COM");
        assertFalse(rateLimiter.isLoginAllowedForEmail("user@test.com"),
                "El email en minúsculas debe contar los intentos anteriores");
    }

    @Test
    void loginByEmail_nullOrBlankEmailAlwaysAllowed() {
        assertTrue(rateLimiter.isLoginAllowedForEmail(null));
        assertTrue(rateLimiter.isLoginAllowedForEmail("   "));
    }

    @Test
    void loginByEmail_differentEmailsAreIndependent() {
        rateLimiter.isLoginAllowedForEmail("victim@test.com");
        rateLimiter.isLoginAllowedForEmail("victim@test.com");
        assertTrue(rateLimiter.isLoginAllowedForEmail("other@test.com"),
                "Un email diferente no debe verse afectado");
    }

    // ── Registro por IP ─────────────────────────────────────────────────────

    @Test
    void registerByIp_allowsUpToLimit() {
        assertTrue(rateLimiter.isRegisterAllowedForIp("5.5.5.5"));
        assertTrue(rateLimiter.isRegisterAllowedForIp("5.5.5.5"));
    }

    @Test
    void registerByIp_blocksWhenLimitExceeded() {
        rateLimiter.isRegisterAllowedForIp("5.5.5.5");
        rateLimiter.isRegisterAllowedForIp("5.5.5.5");
        assertFalse(rateLimiter.isRegisterAllowedForIp("5.5.5.5"),
                "El 3er intento de registro debería ser bloqueado");
    }

    // ── Refresh por IP ──────────────────────────────────────────────────────

    @Test
    void refreshByIp_allowsUpToLimit() {
        for (int i = 0; i < 4; i++) {
            assertTrue(rateLimiter.isRefreshAllowedForIp("7.7.7.7"),
                    "Intento " + (i + 1) + " debería ser permitido");
        }
    }

    @Test
    void refreshByIp_blocksWhenLimitExceeded() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.isRefreshAllowedForIp("7.7.7.7");
        }
        assertFalse(rateLimiter.isRefreshAllowedForIp("7.7.7.7"),
                "El 5to intento de refresh debería ser bloqueado");
    }
}
