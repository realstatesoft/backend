package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropertyMediaDto(

        @NotNull(message = "El tipo de media es obligatorio") MediaType type,

        @NotBlank(message = "La URL es obligatoria") String url,

        String thumbnailUrl,

        Boolean isPrimary,

        Integer orderIndex,

        String title) {
}
