package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversionFunnelSeriesPointResponse(
        String periodLabel,
        LocalDate periodStart,
        long views,
        long visits,
        long offers,
        long sales
) {
}
