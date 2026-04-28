package com.openroof.openroof.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class PropertyViewRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration DEDUPLICATION_WINDOW = Duration.ofSeconds(10);

    private final Map<String, AttemptBucket> buckets = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip, String propertyId) {
        if (ip == null || ip.isBlank() || propertyId == null || propertyId.isBlank()) {
            return false;
        }

        Instant now = Instant.now();
        AttemptBucket bucket = buckets.computeIfAbsent(buildKey(ip, propertyId), ignored -> new AttemptBucket());
        return bucket.recordAttempt(now);
    }

    private String buildKey(String ip, String propertyId) {
        return ip + ":" + propertyId;
    }

    private static final class AttemptBucket {
        private final Deque<Instant> attempts = new ArrayDeque<>();
        private Instant lastAcceptedAt;

        synchronized boolean recordAttempt(Instant now) {
            pruneExpired(now);

            if (lastAcceptedAt != null && now.isBefore(lastAcceptedAt.plus(DEDUPLICATION_WINDOW))) {
                return false;
            }

            if (attempts.size() >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }

            attempts.addLast(now);
            lastAcceptedAt = now;
            return true;
        }

        private void pruneExpired(Instant now) {
            Instant cutoff = now.minus(WINDOW);
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.removeFirst();
            }

            if (attempts.isEmpty()) {
                lastAcceptedAt = null;
            }
        }
    }
}
