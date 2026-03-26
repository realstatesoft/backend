package com.openroof.openroof.dto.dashboard;

public record OwnerDashboardStatsResponse(
    CountStatItem myProperties,
    CountStatItem totalVisits,
    CountStatItem inquiries,
    CountStatItem views
) {}
