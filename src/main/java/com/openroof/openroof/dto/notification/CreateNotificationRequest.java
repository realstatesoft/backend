package com.openroof.openroof.dto.notification;

import com.openroof.openroof.model.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateNotificationRequest(

        Long userId,

        @NotNull(message = "El tipo de notificación es obligatorio")
        NotificationType type,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 255, message = "El título no puede exceder 255 caracteres")
        String title,

        @NotBlank(message = "El mensaje es obligatorio")
        String message,

        Map<String, Object> data,

        @Size(max = 500, message = "La URL de acción no puede exceder 500 caracteres")
        String actionUrl
) {
}
