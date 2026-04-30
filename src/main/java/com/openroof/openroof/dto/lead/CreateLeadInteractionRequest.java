package com.openroof.openroof.dto.lead;

import com.openroof.openroof.model.enums.InteractionType;
import jakarta.validation.constraints.NotNull;

/**
 * Registro manual de interacción con un lead (llamada, correo, nota, etc.).
 */
public record CreateLeadInteractionRequest(
        @NotNull(message = "El tipo de interacción es obligatorio") InteractionType type,
        String subject,
        String note
) {
}
