package com.openroof.openroof.dto.dashboard;

import java.util.List;

public record SalesSummaryResponse(
    long totalSold,
    long monthlyCommissions,
    int activeContracts,
    List<MonthlyDataPoint> monthlyData
) {
    public record MonthlyDataPoint(String month, long sales) {}
}
