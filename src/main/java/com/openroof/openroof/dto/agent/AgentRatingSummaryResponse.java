package com.openroof.openroof.dto.agent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AgentRatingSummaryResponse(
        BigDecimal avgRating,
        Integer totalReviews,
        Map<Integer, Long> ratingDistribution,
        List<AgentReviewResponse> latestReviews
) {}
