package com.openroof.openroof.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.openroof.openroof.client.CambiosChacoClient;
import com.openroof.openroof.dto.exchange.ExchangeRateResponse;
import com.openroof.openroof.dto.exchange.ExchangeRatesResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ExchangeRateUnavailableException;
import com.openroof.openroof.parser.CambiosChacoWidgetParser;
import com.openroof.openroof.parser.CambiosChacoWidgetParser.ParsedRate;
import com.openroof.openroof.parser.CambiosChacoWidgetParser.ParsedWidgetRates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio con fallback de cache para exponer cotizaciones sin filtrar el scraping al frontend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private static final long MIN_CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final long MAX_CACHE_TTL_MS = 15 * 60 * 1000L;
    private static final long DEFAULT_CACHE_TTL_MS = 10 * 60 * 1000L;

    private final CambiosChacoClient cambiosChacoClient;
    private final CambiosChacoWidgetParser widgetParser;

    @Value("${exchange-rates.cache-ttl-ms:600000}")
    private long cacheTtlMs;

    @Value("${exchange-rates.source-url:https://www.cambioschaco.com.py/widgets/cotizacion/?lang=es}")
    private String sourceUrl;

    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    @PostConstruct
    void validateCacheTtl() {
        if (cacheTtlMs < MIN_CACHE_TTL_MS || cacheTtlMs > MAX_CACHE_TTL_MS) {
            log.warn("TTL de cotizaciones fuera de rango ({}ms). Se ajusta al valor seguro por defecto ({}ms).",
                    cacheTtlMs, DEFAULT_CACHE_TTL_MS);
            cacheTtlMs = DEFAULT_CACHE_TTL_MS;
        }
    }

    public ExchangeRatesResponse getExchangeRates() {
        CacheEntry snapshot = getOrRefreshSnapshot();
        return new ExchangeRatesResponse(
                snapshot.orderedRates(),
                snapshot.sourceUpdatedAt(),
                snapshot.retrievedAt(),
                sourceUrl);
    }

    public ExchangeRateResponse getExchangeRate(String currencyCode) {
        String normalizedCurrency = normalizeCurrency(currencyCode);
        if (!CambiosChacoWidgetParser.USD.equals(normalizedCurrency)
                && !CambiosChacoWidgetParser.BRL.equals(normalizedCurrency)) {
            throw new BadRequestException("Moneda no soportada: " + currencyCode);
        }
        CacheEntry snapshot = getOrRefreshSnapshot();
        ExchangeRateResponse response = snapshot.rateByCurrency().get(normalizedCurrency);
        if (response == null) {
            throw new BadRequestException("Moneda no soportada: " + currencyCode);
        }
        return response;
    }

    private CacheEntry getOrRefreshSnapshot() {
        CacheEntry current = cache.get();
        if (current != null && !current.isExpired(cacheTtlMs)) {
            return current;
        }

        synchronized (cache) {
            current = cache.get();
            if (current != null && !current.isExpired(cacheTtlMs)) {
                return current;
            }

            try {
                String html = cambiosChacoClient.fetchWidgetHtml();
                ParsedWidgetRates parsed = widgetParser.parse(html);
                CacheEntry refreshed = buildCacheEntry(parsed);
                cache.set(refreshed);
                return refreshed;
            } catch (RuntimeException ex) {
                if (current != null) {
                    log.warn("Fallback a la última cotización válida por error al refrescar: {}", ex.getMessage());
                    return current;
                }
                throw new ExchangeRateUnavailableException("No hay cotizaciones disponibles en este momento", ex);
            }
        }
    }

    private CacheEntry buildCacheEntry(ParsedWidgetRates parsed) {
        LocalDateTime retrievedAt = LocalDateTime.now();
        Map<String, ExchangeRateResponse> rateByCurrency = Map.of(
                CambiosChacoWidgetParser.USD, toResponse(parsed.rates().get(CambiosChacoWidgetParser.USD), parsed.sourceUpdatedAt(), retrievedAt),
                CambiosChacoWidgetParser.BRL, toResponse(parsed.rates().get(CambiosChacoWidgetParser.BRL), parsed.sourceUpdatedAt(), retrievedAt));

        return new CacheEntry(rateByCurrency, List.of(
                rateByCurrency.get(CambiosChacoWidgetParser.USD),
                rateByCurrency.get(CambiosChacoWidgetParser.BRL)),
                parsed.sourceUpdatedAt(),
                retrievedAt);
    }

    private ExchangeRateResponse toResponse(ParsedRate rate, LocalDateTime sourceUpdatedAt, LocalDateTime retrievedAt) {
        if (rate == null) {
            throw new ExchangeRateUnavailableException("Falta una cotización requerida en el widget");
        }

        return new ExchangeRateResponse(
                rate.currencyCode(),
                rate.currencyName(),
                rate.buyRate(),
                rate.sellRate(),
                sourceUpdatedAt,
                retrievedAt,
                sourceUrl);
    }

    private String normalizeCurrency(String currencyCode) {
        if (!StringUtils.hasText(currencyCode)) {
            throw new BadRequestException("La moneda es obligatoria");
        }
        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private record CacheEntry(
            Map<String, ExchangeRateResponse> rateByCurrency,
            List<ExchangeRateResponse> orderedRates,
            LocalDateTime sourceUpdatedAt,
            LocalDateTime retrievedAt) {

        boolean isExpired(long ttlMs) {
            if (ttlMs <= 0) {
                return true;
            }
            return retrievedAt.plus(Duration.ofMillis(ttlMs)).isBefore(LocalDateTime.now());
        }
    }
}
