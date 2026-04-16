package com.openroof.openroof.dto.document;

import com.openroof.openroof.model.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request para que un administrador cambie el estado de un documento.
 */
@Getter
@NoArgsConstructor
public class UpdateDocumentStatusRequest {

    @NotNull(message = "El estado del documento es obligatorio")
    private DocumentStatus documentStatus;

    /** Notas opcionales del administrador (motivo de rechazo, etc.) */
    private String notes;
}
