package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentReviewResponse(
        Long id,
        Long agentId,
        Long reviewerId,
        String reviewerName,
        String reviewerAvatarUrl,
        Long propertyId,
        String propertyAddress,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @JsonProperty("isOwn") boolean isOwn
) {}
