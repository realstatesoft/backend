package com.openroof.openroof.client;

import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.openroof.openroof.exception.ExchangeRateUnavailableException;

/**
 * Cliente HTTP de solo lectura para descargar el widget HTML de Cambios Chaco.
 */
@Component
@Slf4j
public class CambiosChacoClient {

    @Value("${exchange-rates.cambios-chaco.url:https://www.cambioschaco.com.py/widgets/cotizacion/?lang=es}")
    private String widgetUrl;

    @Value("${exchange-rates.http.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${exchange-rates.http.read-timeout-ms:5000}")
    private int readTimeoutMs;

    private RestTemplate restTemplate;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public String fetchWidgetHtml() {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(widgetUrl, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
                throw new ExchangeRateUnavailableException("Cambios Chaco respondió sin contenido");
            }

            return new String(response.getBody(), StandardCharsets.UTF_8);
        } catch (RestClientException ex) {
            log.warn("No se pudo obtener el widget de Cambios Chaco: {}", ex.getMessage());
            throw new ExchangeRateUnavailableException("No se pudo obtener la cotización de Cambios Chaco", ex);
        }
    }
}
