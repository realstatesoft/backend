package com.openroof.openroof.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Schema(description = "Solicitud para suspender un usuario")
public record SuspendUserRequest(

        @Future(message = "La fecha de suspensión debe ser en el futuro")
        @Schema(description = "Fecha hasta la que dura la suspensión. Null = suspensión indefinida")
        LocalDateTime suspendedUntil,

        @NotBlank(message = "El motivo de suspensión es obligatorio")
        @Schema(description = "Motivo de la suspensión", requiredMode = Schema.RequiredMode.REQUIRED)
        String suspensionReason
) {
}
