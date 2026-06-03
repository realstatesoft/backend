package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.AssignmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssignmentStatusResponse(
    Long assignmentId,
    AssignmentStatus status,
    Long agentProfileId,
    String agentName,
    String agentAvatar,
    BigDecimal agentRating,
    Integer agentReviewCount,
    LocalDateTime assignedAt
) {}
