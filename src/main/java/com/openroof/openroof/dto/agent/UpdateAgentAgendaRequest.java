package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.EventType;
import java.time.LocalDateTime;

public record UpdateAgentAgendaRequest(
        EventType eventType,
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String location,
        String notes,
        Long visitId
) {
}
