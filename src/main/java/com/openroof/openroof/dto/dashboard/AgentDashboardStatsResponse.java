package com.openroof.openroof.dto.dashboard;

public record AgentDashboardStatsResponse(
    CountStatItem activeClients,
    CountStatItem totalSales,
    CountStatItem scheduledVisits,
    MoneyStatItem commissions
) {}
