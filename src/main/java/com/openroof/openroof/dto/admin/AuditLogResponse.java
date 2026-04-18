package com.openroof.openroof.dto.admin;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record AuditLogResponse(
        Long id,
        LocalDateTime createdAt,
        Long userId,
        String userEmail,
        String entityType,
        Long entityId,
        String action,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String ipAddress,
        String userAgent) {
}
