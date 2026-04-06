package com.openroof.openroof.dto.dashboard;

import java.util.List;

public record ReportsSummaryResponse(
    MarketMetrics marketMetrics,
    List<TypeDistribution> propertyByType,
    List<MonthlyTrend> monthlyTrend
) {
    public record MarketMetrics(
        long avgPrice,
        double avgPriceTrend,
        long propertiesListed,
        double propertiesListedTrend,
        int avgDaysOnMarket,
        double avgDaysOnMarketTrend,
        int closingRate,
        double closingRateTrend
    ) {}

    public record TypeDistribution(String name, int value) {}

    public record MonthlyTrend(String month, int ventas, int visitas) {}
}
