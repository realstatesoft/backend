package com.openroof.openroof.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estado de suspensión de un usuario")
public record UserSuspensionStatusResponse(

        @Schema(description = "Indica si el usuario está actualmente suspendido")
        boolean suspended,

        @Schema(description = "Fecha hasta la que dura la suspensión. Null si es indefinida o no está suspendido")
        LocalDateTime suspendedUntil,

        @Schema(description = "Motivo de la suspensión. Null si no está suspendido")
        String suspensionReason
) {
}
