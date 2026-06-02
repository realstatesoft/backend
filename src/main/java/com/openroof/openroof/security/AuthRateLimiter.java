package com.openroof.openroof.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rate limiter para los endpoints de autenticación.
 *
 * <p>Mantiene contadores de ventana deslizante por dimensión:
 * <ul>
 *   <li>Login por IP — bloquea fuerza bruta desde una misma dirección</li>
 *   <li>Login por email — bloquea credential-stuffing sobre una cuenta específica</li>
 *   <li>Registro por IP — limita creación masiva de cuentas</li>
 *   <li>Refresh-token por IP — limita abuso del flujo de rotación</li>
 * </ul>
 *
 * <p>Los límites son configurables por ambiente via {@code auth.rate-limit.*}.
 */
@Component
public class AuthRateLimiter {

    @Value("${auth.rate-limit.login.max-per-ip:20}")
    private int loginMaxPerIp;

    @Value("${auth.rate-limit.login.max-per-email:10}")
    private int loginMaxPerEmail;

    @Value("${auth.rate-limit.login.window-minutes:15}")
    private int loginWindowMinutes;

    @Value("${auth.rate-limit.register.max-per-ip:10}")
    private int registerMaxPerIp;

    @Value("${auth.rate-limit.register.window-minutes:60}")
    private int registerWindowMinutes;

    @Value("${auth.rate-limit.refresh.max-per-ip:30}")
    private int refreshMaxPerIp;

    @Value("${auth.rate-limit.refresh.window-minutes:15}")
    private int refreshWindowMinutes;

    private final Map<String, SlidingWindow> loginByIp      = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindow> loginByEmail   = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindow> registerByIp   = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindow> refreshByIp    = new ConcurrentHashMap<>();

    public boolean isLoginAllowedForIp(String ip) {
        return isAllowed(loginByIp, ip, loginMaxPerIp, Duration.ofMinutes(loginWindowMinutes));
    }

    public boolean isLoginAllowedForEmail(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return isAllowed(loginByEmail, email.toLowerCase(), loginMaxPerEmail, Duration.ofMinutes(loginWindowMinutes));
    }

    public boolean isRegisterAllowedForIp(String ip) {
        return isAllowed(registerByIp, ip, registerMaxPerIp, Duration.ofMinutes(registerWindowMinutes));
    }

    public boolean isRefreshAllowedForIp(String ip) {
        return isAllowed(refreshByIp, ip, refreshMaxPerIp, Duration.ofMinutes(refreshWindowMinutes));
    }

    private boolean isAllowed(Map<String, SlidingWindow> windows, String key, int maxRequests, Duration window) {
        if (key == null || key.isBlank()) {
            return true;
        }
        return windows.computeIfAbsent(key, k -> new SlidingWindow()).tryAcquire(Instant.now(), maxRequests, window);
    }

    private static final class SlidingWindow {
        private final Deque<Instant> timestamps = new ArrayDeque<>();

        synchronized boolean tryAcquire(Instant now, int maxRequests, Duration window) {
            prune(now, window);
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }

        private void prune(Instant now, Duration window) {
            Instant cutoff = now.minus(window);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.removeFirst();
            }
        }
    }
}
