package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

public record ClientInteractionResponse(
        Long id,
        Long agentId,
        String type,
        String subject,
        String note,
        String outcome,
        String source,
        LocalDateTime occurredAt,
        LocalDateTime updatedAt
) {
}
