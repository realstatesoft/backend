package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.InteractionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateClientInteractionRequest(
        @NotNull(message = "El tipo de interacción es obligatorio")
        InteractionType type,

        @Size(max = 255, message = "El asunto no puede superar los 255 caracteres")
        String subject,

        String note,

        @Size(max = 100, message = "El outcome no puede superar los 100 caracteres")
        String outcome,

        @PastOrPresent(message = "La fecha de la interacción no puede ser futura")
        LocalDateTime occurredAt
) {
}
