package com.openroof.openroof.dto.exchange;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Respuesta agregada con las cotizaciones relevantes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExchangeRatesResponse(
        List<ExchangeRateResponse> rates,
        LocalDateTime sourceUpdatedAt,
        LocalDateTime retrievedAt,
        String sourceUrl) {
}
