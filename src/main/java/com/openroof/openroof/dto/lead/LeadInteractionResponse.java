package com.openroof.openroof.dto.lead;

import com.openroof.openroof.model.enums.InteractionType;
import java.time.LocalDateTime;

/**
 * DTO para representar una interacción con un Lead (línea de tiempo).
 */
public record LeadInteractionResponse(
        Long id,
        String type,
        String subject,
        String note,
        String performedByName,
        String oldStatusName,
        String newStatusName,
        LocalDateTime createdAt
) {
}
