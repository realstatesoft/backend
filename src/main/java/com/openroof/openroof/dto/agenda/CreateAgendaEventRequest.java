package com.openroof.openroof.dto.agenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateAgendaEventRequest(
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede exceder 255 caracteres")
    String title,

    String description,

    @NotNull(message = "El tipo de evento es obligatorio")
    String eventType,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDateTime startsAt,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDateTime endsAt,

    @Size(max = 500, message = "La ubicación no puede exceder 500 caracteres")
    String location,

    String notes,

    Long visitId
) {}
