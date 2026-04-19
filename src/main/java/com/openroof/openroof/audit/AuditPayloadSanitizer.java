package com.openroof.openroof.audit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Elimina claves sensibles antes de persistir JSON en auditoría.
 */
public final class AuditPayloadSanitizer {

    private static final Set<String> BLOCKED_KEYS = Set.of(
            "password",
            "passwordhash",
            "password_hash",
            "accesstoken",
            "access_token",
            "refreshtoken",
            "refresh_token",
            "token",
            "secret",
            "authorization");

    private AuditPayloadSanitizer() {
    }

    public static Map<String, Object> sanitize(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : source.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String k = e.getKey().toLowerCase(Locale.ROOT);
            if (BLOCKED_KEYS.contains(k)) {
                out.put(e.getKey(), "[redacted]");
            } else {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
