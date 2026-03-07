package com.openroof.openroof.dto.agent;

import java.math.BigDecimal;

public record AgentProfileSummaryResponse(
        Long id,
        String userName,
        String userAvatarUrl,
        String companyName,
        Integer experienceYears,
        String licenseNumber,
        BigDecimal avgRating,
        Integer totalReviews
) {
}
