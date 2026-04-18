package com.openroof.openroof.dto.flag;

import jakarta.validation.constraints.NotBlank;

public record ResolveFlagRequest(

        @NotBlank(message = "Las notas de resolución son obligatorias")
        String resolutionNotes
) {
}
