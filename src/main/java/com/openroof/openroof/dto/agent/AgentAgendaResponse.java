package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

import com.openroof.openroof.model.enums.EventType;

public record AgentAgendaResponse(
        Long id,
        Long agentId,
        Long visitId,
        EventType eventType,
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String location,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
