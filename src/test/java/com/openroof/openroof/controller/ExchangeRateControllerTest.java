package com.openroof.openroof.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.openroof.openroof.config.JacksonConfig;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.exchange.ExchangeRateResponse;
import com.openroof.openroof.dto.exchange.ExchangeRatesResponse;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import com.openroof.openroof.service.ExchangeRateService;

@WebMvcTest(ExchangeRateController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(propertyViewRateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void getExchangeRates_isPublic() throws Exception {
        when(exchangeRateService.getExchangeRates()).thenReturn(sampleAggregateResponse());

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rates[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.rates[1].currencyCode").value("BRL"));
    }

    @Test
    void getExchangeRate_byCurrency_returnsSingleRate() throws Exception {
        when(exchangeRateService.getExchangeRate("usd")).thenReturn(sampleUsdResponse());

        mockMvc.perform(get("/api/exchange-rates/usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("USD"))
                .andExpect(jsonPath("$.data.sellRate").value(6360));
    }

    @Test
    void getExchangeRate_brl_returnsSingleRate() throws Exception {
        when(exchangeRateService.getExchangeRate("brl")).thenReturn(sampleBrlResponse());

        mockMvc.perform(get("/api/exchange-rates/brl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("BRL"))
                .andExpect(jsonPath("$.data.sellRate").value(1260));
    }

    private ExchangeRatesResponse sampleAggregateResponse() {
        return new ExchangeRatesResponse(
                List.of(sampleUsdResponse(), sampleBrlResponse()),
                LocalDateTime.of(2026, 4, 23, 17, 1),
                LocalDateTime.of(2026, 4, 23, 17, 5),
                "https://www.cambioschaco.com.py/widgets/cotizacion/?lang=es");
    }

    private ExchangeRateResponse sampleUsdResponse() {
        return new ExchangeRateResponse(
                "USD",
                "Dólar Americano",
                new BigDecimal("6300"),
                new BigDecimal("6360"),
                LocalDateTime.of(2026, 4, 23, 17, 1),
                LocalDateTime.of(2026, 4, 23, 17, 5),
                "https://www.cambioschaco.com.py/widgets/cotizacion/?lang=es");
    }

    private ExchangeRateResponse sampleBrlResponse() {
        return new ExchangeRateResponse(
                "BRL",
                "Real",
                new BigDecimal("1210"),
                new BigDecimal("1260"),
                LocalDateTime.of(2026, 4, 23, 17, 1),
                LocalDateTime.of(2026, 4, 23, 17, 5),
                "https://www.cambioschaco.com.py/widgets/cotizacion/?lang=es");
    }
}
