package com.openroof.openroof.dto.agent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyAgentSummaryResponse(
        Long agentClientId,
        Long agentId,
        String agentName,
        String agentAvatarUrl,
        String companyName,
        BigDecimal avgRating,
        Integer totalReviews,
        String status,
        LocalDateTime createdAt) {
}
