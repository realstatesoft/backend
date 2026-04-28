package com.openroof.openroof.dto.contract;

import lombok.Builder;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista resumida de contrato para listados (dashboard, tabla de ventas/alquileres).
 */
@Builder
public record ContractSummaryResponse(

        Long id,

        Long propertyId,
        String propertyTitle,

        String buyerName,
        String sellerName,

        String listingAgentName,
        String buyerAgentName,

        ContractType contractType,
        ContractStatus status,

        BigDecimal amount,

        /** Monto total de comisión (calculado: amount * commissionPct / 100) */
        BigDecimal totalCommissionAmount,

        LocalDate startDate,
        LocalDateTime createdAt,

        /** Indica si el usuario que realiza la consulta ya firmó este contrato */
        boolean currentUserHasSigned
) {
}
