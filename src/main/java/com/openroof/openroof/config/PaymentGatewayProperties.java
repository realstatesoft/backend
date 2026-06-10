package com.openroof.openroof.config;

import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la pasarela de pagos (bloque {@code payments} en application.yml).
 * El paso a producción con Bancard es solo cambiar variables de entorno:
 * PAYMENT_GATEWAY=BANCARD + BANCARD_BASE_URL + llaves.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "payments")
public class PaymentGatewayProperties {

    /** MOCK | BANCARD */
    private PaymentGatewayProvider gateway = PaymentGatewayProvider.MOCK;

    private final Bancard bancard = new Bancard();
    private final Checkout checkout = new Checkout();
    private final Mock mock = new Mock();
    private final Reconciliation reconciliation = new Reconciliation();

    @Getter
    @Setter
    public static class Bancard {
        /** Staging: https://vpos.infonet.com.py:8888 · Producción: https://vpos.infonet.com.py */
        private String baseUrl = "https://vpos.infonet.com.py:8888";
        private String publicKey = "";
        private String privateKey = "";
    }

    @Getter
    @Setter
    public static class Checkout {
        /** URLs por defecto si el cliente no envía return_url/cancel_url en el body. */
        private String returnUrl = "http://localhost:5173/payments/result";
        private String cancelUrl = "http://localhost:5173/payments/cancelled";
    }

    @Getter
    @Setter
    public static class Mock {
        /** URL del webhook propio que el mock invoca para confirmar, igual que lo haría Bancard. */
        private String webhookUrl = "http://localhost:8080/api/payments/webhooks/bancard";
        private long confirmDelayMs = 2000;
    }

    @Getter
    @Setter
    public static class Reconciliation {
        /** Minutos sin confirmación antes de consultar/reversar (la spec recomienda 10). */
        private int staleAfterMinutes = 10;
    }
}
