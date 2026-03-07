package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.SocialMediaPlatform;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentSocialMediaDto(

        @NotNull(message = "La plataforma es obligatoria")
        SocialMediaPlatform platform,

        @NotNull(message = "La URL es obligatoria")
        @Size(max = 500, message = "La URL no puede exceder 500 caracteres")
        String url
) {
}
