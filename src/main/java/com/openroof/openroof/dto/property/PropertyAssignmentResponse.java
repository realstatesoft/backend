package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.AssignmentStatus;

import java.time.LocalDateTime;

public record PropertyAssignmentResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        Long agentId,
        String agentName,
        Long assignedById,
        String assignedByName,
        AssignmentStatus status,
        LocalDateTime assignedAt,
        LocalDateTime createdAt
) {}
