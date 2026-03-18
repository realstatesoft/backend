package com.openroof.openroof.dto.visit;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateVisitRequestRequest(

        @NotNull(message = "El ID de la propiedad es obligatorio")
        Long propertyId,

        @NotNull(message = "La fecha propuesta es obligatoria")
        @Future(message = "La fecha propuesta debe ser futura")
        LocalDateTime proposedAt,

        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String buyerName,

        @Size(max = 255, message = "El email no puede exceder 255 caracteres")
        String buyerEmail,

        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        String buyerPhone,

        String message
) {}
