package com.openroof.openroof.dto.contract;

import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta completa de un contrato, incluyendo montos de comisión calculados.
 */
public record ContractResponse(

        Long id,

        // ─── Propiedad ────────────────────────────────────────────
        Long propertyId,
        String propertyTitle,

        // ─── Partes del contrato ──────────────────────────────────
        Long buyerId,
        String buyerName,
        String buyerEmail,

        Long sellerId,
        String sellerName,
        String sellerEmail,

        // ─── Agentes ──────────────────────────────────────────────
        Long listingAgentId,
        String listingAgentName,

        Long buyerAgentId,
        String buyerAgentName,

        // ─── Detalles del contrato ────────────────────────────────
        ContractType contractType,
        ContractStatus status,
        BigDecimal amount,

        LocalDate startDate,
        LocalDate endDate,

        String terms,
        String documentUrl,

        // ─── Comisiones (% configurados) ──────────────────────────
        BigDecimal commissionPct,
        BigDecimal listingAgentCommissionPct,
        BigDecimal buyerAgentCommissionPct,

        // ─── Comisiones (montos calculados) ───────────────────────
        /** amount * commissionPct / 100 */
        BigDecimal totalCommissionAmount,
        /** amount * listingAgentCommissionPct / 100 */
        BigDecimal listingAgentCommissionAmount,
        /** amount * buyerAgentCommissionPct / 100 */
        BigDecimal buyerAgentCommissionAmount,

        // ─── Auditoría ────────────────────────────────────────────
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
