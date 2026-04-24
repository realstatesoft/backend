package com.openroof.openroof.dto.exchange;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cotización de una moneda frente al PYG.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExchangeRateResponse(
        String currencyCode,
        String currencyName,
        BigDecimal buyRate,
        BigDecimal sellRate,
        LocalDateTime sourceUpdatedAt,
        LocalDateTime retrievedAt,
        String sourceUrl) {
}
