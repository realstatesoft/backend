package com.openroof.openroof.dto.dashboard;

public record AgentDashboardStatsResponse(
    StatItem activeClients,
    StatItem totalSales,
    StatItem scheduledVisits,
    StatItem commissions
) {}
