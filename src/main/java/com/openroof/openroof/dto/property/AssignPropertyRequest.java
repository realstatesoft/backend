package com.openroof.openroof.dto.property;

import jakarta.validation.constraints.NotNull;

public record AssignPropertyRequest(
        @NotNull(message = "El ID del perfil de agente es obligatorio")
        Long agentProfileId
) {}
