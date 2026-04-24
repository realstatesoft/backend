package com.openroof.openroof.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openroof.openroof.client.CambiosChacoClient;
import com.openroof.openroof.dto.exchange.ExchangeRateResponse;
import com.openroof.openroof.dto.exchange.ExchangeRatesResponse;
import com.openroof.openroof.exception.ExchangeRateUnavailableException;
import com.openroof.openroof.parser.CambiosChacoWidgetParser;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private CambiosChacoClient cambiosChacoClient;

    @Mock
    private CambiosChacoWidgetParser widgetParser;

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        service = new ExchangeRateService(cambiosChacoClient, widgetParser);
        setField("cacheTtlMs", 10 * 60 * 1000L);
        setField("sourceUrl", "https://www.cambioschaco.com.py/widgets/cotizacion/?lang=es");
    }

    @Test
    void getExchangeRates_fetchesAndCachesSnapshot() {
        CambiosChacoWidgetParser.ParsedWidgetRates parsed = sampleParsedRates();
        when(cambiosChacoClient.fetchWidgetHtml()).thenReturn("<html />");
        when(widgetParser.parse(anyString())).thenReturn(parsed);

        ExchangeRatesResponse first = service.getExchangeRates();
        ExchangeRatesResponse second = service.getExchangeRates();

        assertThat(first.rates()).hasSize(2);
        assertThat(second.rates()).hasSize(2);
        verify(cambiosChacoClient, times(1)).fetchWidgetHtml();
        verify(widgetParser, times(1)).parse(anyString());
    }

    @Test
    void getExchangeRate_returnsRequestedCurrency() {
        CambiosChacoWidgetParser.ParsedWidgetRates parsed = sampleParsedRates();
        when(cambiosChacoClient.fetchWidgetHtml()).thenReturn("<html />");
        when(widgetParser.parse(anyString())).thenReturn(parsed);

        ExchangeRateResponse usd = service.getExchangeRate("usd");

        assertThat(usd.currencyCode()).isEqualTo("USD");
        assertThat(usd.sellRate()).isEqualByComparingTo("6360");
    }

    @Test
    void getExchangeRates_returnsLastValidSnapshotWhenRefreshFails() {
        CambiosChacoWidgetParser.ParsedWidgetRates parsed = sampleParsedRates();
        when(cambiosChacoClient.fetchWidgetHtml()).thenReturn("<html />").thenThrow(new RuntimeException("upstream down"));
        when(widgetParser.parse(anyString())).thenReturn(parsed);
        setField("cacheTtlMs", 0L);

        ExchangeRatesResponse first = service.getExchangeRates();
        ExchangeRatesResponse second = service.getExchangeRates();

        assertThat(first.rates()).hasSize(2);
        assertThat(second.rates()).hasSize(2);
        verify(cambiosChacoClient, times(2)).fetchWidgetHtml();
    }

    @Test
    void getExchangeRates_withoutCacheAndUpstreamFailure_throwsUnavailable() {
        when(cambiosChacoClient.fetchWidgetHtml()).thenThrow(new RuntimeException("upstream down"));

        assertThatThrownBy(() -> service.getExchangeRates())
                .isInstanceOf(ExchangeRateUnavailableException.class);

        verify(widgetParser, never()).parse(anyString());
    }

    private CambiosChacoWidgetParser.ParsedWidgetRates sampleParsedRates() {
        return new CambiosChacoWidgetParser.ParsedWidgetRates(
                Map.of(
                        CambiosChacoWidgetParser.USD, new CambiosChacoWidgetParser.ParsedRate("USD", "Dólar Americano", new BigDecimal("6300"), new BigDecimal("6360")),
                        CambiosChacoWidgetParser.BRL, new CambiosChacoWidgetParser.ParsedRate("BRL", "Real", new BigDecimal("1210"), new BigDecimal("1260"))),
                LocalDateTime.of(2026, 4, 23, 17, 1));
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = ExchangeRateService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
