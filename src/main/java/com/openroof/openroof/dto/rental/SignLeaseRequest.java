package com.openroof.openroof.dto.rental;

/**
 * Payload opcional para capturar evidencia adicional de la firma.
 * El firmante se resuelve por token, no por este request.
 */
public record SignLeaseRequest(
        String signatureData
) {
}
