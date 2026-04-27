package com.openroof.openroof.dto.lead;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta para un Lead.
 */
public record LeadResponse(
        Long id,
        Long agentId,
        String agentName,
        Long userId,
        String name,
        String email,
        String phone,
        String source,
        String status,
        String statusColor,
        String notes,
        Map<String, Object> metadata,
        java.util.List<LeadInteractionResponse> interactions,
        LocalDateTime createdAt
) {
}
