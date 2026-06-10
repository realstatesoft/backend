package com.openroof.openroof.config;

import com.openroof.openroof.gateway.BancardGateway;
import com.openroof.openroof.gateway.MockPaymentGateway;
import com.openroof.openroof.gateway.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestClient;

/**
 * Selección de la pasarela de pagos por configuración ({@code payments.gateway}).
 * MOCK en dev/test; BANCARD en producción cambiando solo variables de entorno.
 */
@Configuration
@EnableConfigurationProperties(PaymentGatewayProperties.class)
@Slf4j
public class PaymentGatewayConfig {

    @Bean
    public PaymentGateway paymentGateway(PaymentGatewayProperties properties, TaskScheduler taskScheduler) {
        RestClient restClient = RestClient.create();
        PaymentGateway gateway = switch (properties.getGateway()) {
            case MOCK -> new MockPaymentGateway(properties, taskScheduler, restClient);
            case BANCARD -> new BancardGateway(properties, restClient);
        };
        log.info("Pasarela de pagos activa: {} (base-url Bancard: {})",
                properties.getGateway(), properties.getBancard().getBaseUrl());
        return gateway;
    }
}
