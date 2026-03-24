package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.EventType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateAgentAgendaRequest(
        @NotNull(message = "El tipo de evento es obligatorio")
        EventType eventType,

        @NotBlank(message = "El título es obligatorio")
        String title,

        String description,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime startsAt,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDateTime endsAt,

        String location,

        String notes,

        Long visitId
) {
    @AssertTrue(message = "La fecha de fin debe ser posterior a la fecha de inicio")
    public boolean isEndsAtAfterStartsAt() {
        if (startsAt == null || endsAt == null) return true;
        return endsAt.isAfter(startsAt);
    }
}
