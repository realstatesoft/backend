package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversionFunnelSummaryResponse(
        ConversionFunnelStagesResponse current,
        ConversionFunnelRatesResponse rates,
        ConversionFunnelStagesResponse previousPeriod,
        ConversionFunnelRatesResponse previousRates,
        ConversionFunnelKpisResponse kpis,
        List<ConversionFunnelSeriesPointResponse> series
) {
}
