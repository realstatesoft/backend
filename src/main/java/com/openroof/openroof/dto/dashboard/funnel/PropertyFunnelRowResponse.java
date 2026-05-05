package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyFunnelRowResponse(
        long propertyId,
        String title,
        String address,
        String status,
        BigDecimal price,
        long views,
        long visits,
        long offers,
        long sales,
        Double conversionFromViewsPct
) {
}
