package com.openroof.openroof.dto.lead;

import com.openroof.openroof.model.enums.InteractionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro manual de interacción con un lead (llamada, correo, nota, etc.).
 */
public record CreateLeadInteractionRequest(
        @NotNull(message = "El tipo de interacción es obligatorio") InteractionType type,
        @Size(max = 255, message = "El asunto no puede superar 255 caracteres") String subject,
        String note
) {
}
