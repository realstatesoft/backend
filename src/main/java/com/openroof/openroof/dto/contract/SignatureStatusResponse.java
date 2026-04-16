package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.SignatureRole;
import com.openroof.openroof.model.enums.SignatureType;

import java.time.LocalDateTime;

/**
 * Respuesta con el estado de una firma dentro de un contrato.
 */
public record SignatureStatusResponse(
        Long signatureId,
        Long signerId,
        String signerName,
        String signerEmail,
        SignatureRole role,
        SignatureType signatureType,
        LocalDateTime signedAt,
        boolean signed
) {
}
