package com.openroof.openroof.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
    @NotNull(message = "El destinatario es obligatorio")
    Long receiverId,

    @NotBlank(message = "El mensaje no puede estar vacío")
    String content,

    Long propertyId
) {}
