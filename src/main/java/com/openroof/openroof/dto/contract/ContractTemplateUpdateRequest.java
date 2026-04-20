package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Actualización completa de plantilla (ADMIN).
 */
public record ContractTemplateUpdateRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        String name,

        @NotNull(message = "El tipo de contrato es obligatorio")
        ContractType contractType,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 100_000, message = "El contenido no puede superar 100000 caracteres")
        String content,

        @NotBlank(message = "La versión es obligatoria")
        @Size(max = 20, message = "La versión no puede superar 20 caracteres")
        String templateVersion,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
}
