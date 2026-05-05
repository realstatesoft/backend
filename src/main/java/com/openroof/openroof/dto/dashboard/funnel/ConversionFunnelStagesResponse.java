package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversionFunnelStagesResponse(
        long views,
        long visits,
        long offers,
        long sales
) {
}
