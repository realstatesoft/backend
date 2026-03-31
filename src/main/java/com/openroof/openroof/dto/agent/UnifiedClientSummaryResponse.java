package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

public record UnifiedClientSummaryResponse(
        Long id,
        Long userId, // null for ExternalClient
        String name,
        String email,
        String phone,
        String status,
        String priority,
        String clientType,
        LocalDateTime lastContactDate,
        LocalDateTime createdAt,
        String internalType // "AGENT" or "EXTERNAL"
) {
}
