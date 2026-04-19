package com.openroof.openroof.dto.document;

import com.openroof.openroof.model.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    /** Notas opcionales del administrador (motivo de rechazo, etc.). Máximo 2000 caracteres. */
    @Size(max = 2000, message = "Las notas no pueden superar los 2000 caracteres")
    private String notes;
}
