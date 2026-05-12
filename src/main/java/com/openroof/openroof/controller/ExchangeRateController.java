package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.exchange.ExchangeRateResponse;
import com.openroof.openroof.dto.exchange.ExchangeRatesResponse;
import com.openroof.openroof.service.ExchangeRateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público para consultar cotizaciones PYG vs USD/BRL.
 */
@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Cotizaciones públicas de Cambios Chaco")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @Operation(summary = "Obtener cotizaciones disponibles")
    public ResponseEntity<ApiResponse<ExchangeRatesResponse>> getExchangeRates() {
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.getExchangeRates()));
    }

    @GetMapping("/{currency}")
    @Operation(summary = "Obtener una cotización específica (USD o BRL)")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getExchangeRate(@PathVariable String currency) {
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.getExchangeRate(currency)));
    }
}
