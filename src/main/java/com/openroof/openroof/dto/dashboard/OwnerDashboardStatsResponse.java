package com.openroof.openroof.dto.dashboard;

public record OwnerDashboardStatsResponse(
    StatItem myProperties,
    StatItem totalVisits,
    StatItem inquiries,
    StatItem views
) {}
