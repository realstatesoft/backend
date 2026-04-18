package com.openroof.openroof.dto.flag;

import com.openroof.openroof.model.enums.FlagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFlagRequest(

        @NotNull(message = "El tipo de reporte es obligatorio")
        FlagType flagType,

        @NotBlank(message = "La razón del reporte es obligatoria")
        @Size(max = 1000, message = "La razón no puede exceder 1000 caracteres")
        String reason
) {
}
