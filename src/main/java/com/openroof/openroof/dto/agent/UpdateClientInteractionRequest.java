package com.openroof.openroof.dto.agent;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateClientInteractionRequest(
        @Size(max = 255, message = "El asunto no puede superar los 255 caracteres")
        String subject,

        String note,

        @Size(max = 100, message = "El outcome no puede superar los 100 caracteres")
        String outcome,

        LocalDateTime occurredAt
) {
}
