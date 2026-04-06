package com.openroof.openroof.dto.notification;

import com.openroof.openroof.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        Map<String, Object> data,
        String actionUrl,
        LocalDateTime readAt,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
