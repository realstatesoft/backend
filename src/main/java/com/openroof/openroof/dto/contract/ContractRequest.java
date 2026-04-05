package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para crear un contrato de compraventa o alquiler.
 *
 * Escenarios de comisión soportados:
 *  A) Directo propietario→usuario: listingAgentId=null, buyerAgentId=null, todos los pct=0
 *  B) Un solo agente (listador): listingAgentId=X, buyerAgentId=null, buyerAgentCommissionPct=0
 *  C) Un solo agente (comprador): listingAgentId=null, buyerAgentId=Y, listingAgentCommissionPct=0
 *  D) Dual agency: listingAgentId=X, buyerAgentId=Y, pcts > 0 en ambos
 *
 * Invariante: commissionPct == listingAgentCommissionPct + buyerAgentCommissionPct
 */
public record ContractRequest(

        @NotNull(message = "La propiedad es obligatoria")
        Long propertyId,

        @NotNull(message = "El comprador/inquilino es obligatorio")
        Long buyerId,

        @NotNull(message = "El vendedor/propietario es obligatorio")
        Long sellerId,

        /** Agente del vendedor/propietario. Null si no hay intermediario del lado vendedor. */
        Long listingAgentId,

        /** Agente del comprador/inquilino. Null si no hay intermediario del lado comprador. */
        Long buyerAgentId,

        @NotNull(message = "El tipo de contrato es obligatorio")
        ContractType contractType,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount,

        /**
         * Comisión total de la operación en porcentaje.
         * Debe ser igual a listingAgentCommissionPct + buyerAgentCommissionPct.
         * Usar 0 cuando no hay agentes intermediarios.
         */
        @DecimalMin(value = "0.0", message = "La comisión no puede ser negativa")
        @DecimalMax(value = "100.0", message = "La comisión no puede superar el 100%")
        BigDecimal commissionPct,

        /**
         * Porcentaje del monto que corresponde al agente listador.
         * Debe ser 0 si listingAgentId es null.
         */
        @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El porcentaje no puede superar el 100%")
        BigDecimal listingAgentCommissionPct,

        /**
         * Porcentaje del monto que corresponde al agente del comprador.
         * Debe ser 0 si buyerAgentId es null.
         */
        @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo")
        @DecimalMax(value = "100.0", message = "El porcentaje no puede superar el 100%")
        BigDecimal buyerAgentCommissionPct,

        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 5000, message = "Los términos no pueden superar 5000 caracteres")
        String terms,

        Long templateId
) {
}
