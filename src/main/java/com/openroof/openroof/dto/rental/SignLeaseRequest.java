package com.openroof.openroof.dto.rental;

import jakarta.validation.constraints.Size;

/**
 * Payload opcional para capturar evidencia adicional de la firma.
 * El firmante se resuelve por token, no por este request.
 * signatureData se limita para evitar payloads desmedidos en auditoría.
 */
public record SignLeaseRequest(
        @Size(max = 4096, message = "signatureData must be at most 4096 characters")
        String signatureData
) {
}
