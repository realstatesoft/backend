package com.openroof.openroof.dto.dashboard.funnel;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tasas entre etapas consecutivas (porcentajes 0–100, null si el denominador es 0).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversionFunnelRatesResponse(
        Double viewsToVisitsPct,
        Double visitsToOffersPct,
        Double offersToSalesPct,
        Double viewsToSalesPct
) {
}
