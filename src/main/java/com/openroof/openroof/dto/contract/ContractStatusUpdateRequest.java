package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request para actualizar el estado de un contrato.
 * Transiciones válidas:
 *   DRAFT      → SENT, CANCELLED
 *   SENT       → PARTIALLY_SIGNED, REJECTED, CANCELLED
 *   PARTIALLY_SIGNED → SIGNED, REJECTED, CANCELLED
 *   SIGNED     → EXPIRED (solo admin, por proceso automático)
 */
public record ContractStatusUpdateRequest(

        @NotNull(message = "El estado es obligatorio")
        ContractStatus status
) {
}
