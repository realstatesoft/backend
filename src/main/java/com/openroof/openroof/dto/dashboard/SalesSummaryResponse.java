package com.openroof.openroof.dto.dashboard;

import java.util.List;

public record SalesSummaryResponse(
    long totalSold,
    long myCommissions,
    int signedContracts,
    int activeContracts,
    List<MonthlyDataPoint> monthlyData
) {
    public record MonthlyDataPoint(String month, long sales, long myCommissions) {}
}
