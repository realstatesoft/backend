package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyFunnelPageResponse(
        List<PropertyFunnelRowResponse> content,
        long totalElements,
        int page,
        int size
) {
}
