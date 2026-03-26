package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

public record AgentClientSummaryResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String userPhone,
        String status,
        String priority,
        String clientType,
        LocalDateTime lastContactAt,
        LocalDateTime createdAt) {
}
