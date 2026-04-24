package com.openroof.openroof.dto.agent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AgentProfileResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String userPhone,
        String userAvatarUrl,
        String companyName,
        String bio,
        Integer experienceYears,
        String licenseNumber,
        BigDecimal avgRating,
        Integer totalReviews,
        List<SpecialtyDto> specialties,
        List<AgentSocialMediaDto> socialMedia,
        AgentStatsDto stats,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record SpecialtyDto(Long id, String name) {
    }

    public record AgentStatsDto(
            Integer vendidas,
            Integer alquiladas,
            Integer total,
            String precioPromedio
    ) {
    }
}
