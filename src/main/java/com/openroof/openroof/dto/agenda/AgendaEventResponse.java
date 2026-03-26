package com.openroof.openroof.dto.agenda;

import java.time.LocalDateTime;

public record AgendaEventResponse(
    Long id,
    String title,
    String description,
    String eventType,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String location,
    String notes,
    String clientName,
    LocalDateTime createdAt
) {}
