package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

public record AgentReviewResponse(
        Long id,
        Long agentId,
        Long reviewerId,
        String reviewerName,
        Long propertyId,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
