package com.openroof.openroof.dto.visit;

import com.openroof.openroof.model.enums.VisitRequestStatus;

import java.time.LocalDateTime;

public record VisitRequestResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        Long buyerId,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        Long agentId,
        String agentName,
        LocalDateTime proposedAt,
        LocalDateTime counterProposedAt,
        String counterProposeMessage,
        VisitRequestStatus status,
        String message,
        Long visitId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
