package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.SignatureRole;
import com.openroof.openroof.model.enums.SignatureType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Request para firmar un contrato.
 * El contrato debe estar en estado SENT o PARTIALLY_SIGNED.
 * El firmante debe ser una parte legítima del contrato.
 */
public record SignContractRequest(

        @NotNull(message = "El tipo de firma es obligatorio")
        SignatureType signatureType,

        @NotNull(message = "El rol del firmante es obligatorio")
        SignatureRole role,

        /**
         * Datos de la firma:
         *  - ELECTRONIC: puede ser null o un simple token de confirmación
         *  - DIGITAL: hash/certificado del firmante
         *  - HANDWRITTEN_SCAN: base64 de la imagen escaneada
         */
        String signatureData
) {
        @AssertTrue(message = "El campo signatureData es obligatorio para firmas DIGITAL o HANDWRITTEN_SCAN")
        public boolean isSignatureDataValid() {
                if (signatureType == SignatureType.DIGITAL || signatureType == SignatureType.HANDWRITTEN_SCAN) {
                        return signatureData != null && !signatureData.trim().isEmpty();
                }
                return true;
        }
}
