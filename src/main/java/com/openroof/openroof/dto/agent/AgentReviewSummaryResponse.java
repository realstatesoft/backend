package com.openroof.openroof.dto.agent;

import java.math.BigDecimal;

public record AgentReviewSummaryResponse(
        Long agentId,
        BigDecimal avgRating,
        long reviewCount
) {}
