package com.openroof.openroof.dto.agent;

import java.math.BigDecimal;
import java.util.List;

public record AgentProfileSummaryResponse(
        Long id,
        String userName,
        String userPhone,
        String userAvatarUrl,
        String companyName,
        Integer experienceYears,
        String licenseNumber,
        BigDecimal avgRating,
        Integer totalReviews,
        List<String> specialties
) {
}
