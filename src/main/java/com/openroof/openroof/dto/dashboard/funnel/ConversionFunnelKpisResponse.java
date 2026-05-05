package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversionFunnelKpisResponse(
        Double globalConversionPct,
        Double medianDaysViewToSale,
        BigDecimal closedVolume,
        Double globalConversionPctDeltaPp,
        Double medianDaysViewToSaleDelta,
        Double closedVolumePctChange
) {
}
