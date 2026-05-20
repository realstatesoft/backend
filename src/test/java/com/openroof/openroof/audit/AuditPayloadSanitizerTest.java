package com.openroof.openroof.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code AuditPayloadSanitizer}.
 */
@DisplayName("AuditPayloadSanitizer")
class AuditPayloadSanitizerTest {

    // ─── null / empty ──────────────────────────────────────────────────────────

    /**
     * sanitize_nullSource_returnsNull.
     */
    @Test
    @DisplayName("sanitize_nullSource_returnsNull")
    void sanitize_nullSource_returnsNull() {
        assertThat(AuditPayloadSanitizer.sanitize(null)).isNull();
    }

    /**
     * sanitize_emptyMap_returnsEmptyMap.
     */
    @Test
    @DisplayName("sanitize_emptyMap_returnsEmptyMap")
    void sanitize_emptyMap_returnsEmptyMap() {
        Map<String, Object> result = AuditPayloadSanitizer.sanitize(new HashMap<>());
        assertThat(result).isEmpty();
    }

    // ─── blocked keys (lowercase exact matches) ───────────────────────────────

    /**
     * sanitize_passwordKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_passwordKey_isRedacted")
    void sanitize_passwordKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("password", "mySecret123");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("password", "[redacted]");
    }

    /**
     * sanitize_passwordHashWithUnderscore_isRedacted.
     */
    @Test
    @DisplayName("sanitize_passwordHashWithUnderscore_isRedacted")
    void sanitize_passwordHashWithUnderscore_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("password_hash", "bcrypt$hash$value");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("password_hash", "[redacted]");
    }

    /**
     * sanitize_accessTokenKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_accessTokenKey_isRedacted")
    void sanitize_accessTokenKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("accesstoken", "Bearer eyJhbGc...");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("accesstoken", "[redacted]");
    }

    /**
     * sanitize_accessTokenWithUnderscore_isRedacted.
     */
    @Test
    @DisplayName("sanitize_accessTokenWithUnderscore_isRedacted")
    void sanitize_accessTokenWithUnderscore_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("access_token", "eyJhbGc...");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("access_token", "[redacted]");
    }

    /**
     * sanitize_refreshTokenKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_refreshTokenKey_isRedacted")
    void sanitize_refreshTokenKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("refresh_token", "rt_abc123");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("refresh_token", "[redacted]");
    }

    /**
     * sanitize_tokenKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_tokenKey_isRedacted")
    void sanitize_tokenKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("token", "tk_xyz789");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("token", "[redacted]");
    }

    /**
     * sanitize_secretKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_secretKey_isRedacted")
    void sanitize_secretKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("secret", "supersecret");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("secret", "[redacted]");
    }

    /**
     * sanitize_authorizationKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_authorizationKey_isRedacted")
    void sanitize_authorizationKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("authorization", "Bearer token123");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("authorization", "[redacted]");
    }

    // ─── case-insensitive matching ─────────────────────────────────────────────

    /**
     * sanitize_upperCasePasswordKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_upperCasePasswordKey_isRedacted")
    void sanitize_upperCasePasswordKey_isRedacted() {
        // Key "PASSWORD" lowercased = "password" → blocked
        Map<String, Object> input = new HashMap<>();
        input.put("PASSWORD", "mySecret");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("PASSWORD", "[redacted]");
    }

    /**
     * sanitize_mixedCaseTokenKey_isRedacted.
     */
    @Test
    @DisplayName("sanitize_mixedCaseTokenKey_isRedacted")
    void sanitize_mixedCaseTokenKey_isRedacted() {
        Map<String, Object> input = new HashMap<>();
        input.put("Token", "abc123");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("Token", "[redacted]");
    }

    // ─── non-sensitive keys pass through ──────────────────────────────────────

    /**
     * sanitize_nonSensitiveKey_passesThrough.
     */
    @Test
    @DisplayName("sanitize_nonSensitiveKey_passesThrough")
    void sanitize_nonSensitiveKey_passesThrough() {
        Map<String, Object> input = new HashMap<>();
        input.put("username", "john_doe");
        input.put("email", "john@example.com");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("username", "john_doe");
        assertThat(result).containsEntry("email", "john@example.com");
    }

    /**
     * sanitize_mixedMap_redactsOnlySensitiveKeys.
     */
    @Test
    @DisplayName("sanitize_mixedMap_redactsOnlySensitiveKeys")
    void sanitize_mixedMap_redactsOnlySensitiveKeys() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 42);
        input.put("email", "user@example.com");
        input.put("password", "plaintext!");
        input.put("action", "UPDATE_PROFILE");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).containsEntry("password", "[redacted]");
        assertThat(result).containsEntry("userId", 42);
        assertThat(result).containsEntry("email", "user@example.com");
        assertThat(result).containsEntry("action", "UPDATE_PROFILE");
        assertThat(result).hasSize(4);
    }

    // ─── null key is skipped ──────────────────────────────────────────────────

    /**
     * sanitize_nullKey_isSkipped.
     */
    @Test
    @DisplayName("sanitize_nullKey_isSkipped")
    void sanitize_nullKey_isSkipped() {
        Map<String, Object> input = new HashMap<>();
        input.put(null, "some value");
        input.put("username", "alice");

        Map<String, Object> result = AuditPayloadSanitizer.sanitize(input);

        assertThat(result).doesNotContainKey(null);
        assertThat(result).containsEntry("username", "alice");
    }
}
