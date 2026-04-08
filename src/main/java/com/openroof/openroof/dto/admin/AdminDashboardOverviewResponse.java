package com.openroof.openroof.dto.admin;

import java.util.List;

public record AdminDashboardOverviewResponse(
        AdminKpiDto users,
        AdminKpiDto properties,
        AdminKpiDto transactions,
        AdminKpiDto revenue,
        List<AdminQuickActionDto> quickActions,
        List<AdminActivityItemDto> recentActivity,
        List<AdminAttentionItemDto> attentionItems) {
}
