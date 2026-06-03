package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.AssignmentStatus;

import java.time.LocalDateTime;

public record PropertyAssignmentResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        String propertyAddress,
        String propertyImage,
        Long agentProfileId,
        Long agentUserId,
        String agentName,
        Long ownerId,
        String ownerName,
        AssignmentStatus status,
        LocalDateTime assignedAt
) {}
