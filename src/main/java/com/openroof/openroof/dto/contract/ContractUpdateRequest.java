package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para actualizar un contrato en estado DRAFT.
 * Mismas reglas de comisión que ContractRequest:
 *   commissionPct == listingAgentCommissionPct + buyerAgentCommissionPct
 */
public record ContractUpdateRequest(

        Long listingAgentId,

        Long buyerAgentId,

        @NotNull(message = "El tipo de contrato es obligatorio")
        ContractType contractType,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount,

        @DecimalMin(value = "0.0", message = "La comisión no puede ser negativa")
        @DecimalMax(value = "100.0", message = "La comisión no puede superar el 100%")
        BigDecimal commissionPct,

        @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El porcentaje no puede superar el 100%")
        BigDecimal listingAgentCommissionPct,

        @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El porcentaje no puede superar el 100%")
        BigDecimal buyerAgentCommissionPct,

        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 5000, message = "Los términos no pueden superar 5000 caracteres")
        String terms
) {
}
