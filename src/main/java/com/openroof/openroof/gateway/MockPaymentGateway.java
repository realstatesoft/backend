package com.openroof.openroof.gateway;

import com.openroof.openroof.config.PaymentGatewayProperties;
import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import com.openroof.openroof.model.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pasarela simulada para dev/test. Imita el contrato exacto de Bancard vPOS:
 * devuelve un process_id y, tras un delay corto, confirma invocando el webhook
 * propio con el JSON real de single_buy_confirm (token md5 incluido).
 *
 * Reglas determinísticas para QA (sobre la parte entera del monto):
 *   monto terminado en 99 → response_code "51" (fondos insuficientes)
 *   monto terminado en 15 → response_code "15" (tarjeta inválida)
 *   resto                 → response_code "00" (aprobada)
 */
@Slf4j
@RequiredArgsConstructor
public class MockPaymentGateway implements PaymentGateway {

    private static final String CURRENCY = "PYG";

    private final PaymentGatewayProperties properties;
    private final TaskScheduler taskScheduler;
    private final RestClient restClient;

    @Override
    public PaymentGatewayProvider provider() {
        return PaymentGatewayProvider.MOCK;
    }

    @Override
    public GatewayCheckoutResult createCheckout(Payment payment, String returnUrl, String cancelUrl) {
        String processId = "mock-" + payment.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        GatewayConfirmation confirmation = buildConfirmation(payment);
        long delayMs = properties.getMock().getConfirmDelayMs();
        taskScheduler.schedule(() -> postConfirmation(confirmation), Instant.now().plusMillis(delayMs));
        log.info("[MOCK vPOS] single_buy pago id={} → process_id={} (confirmación en {} ms, response_code={})",
                payment.getId(), processId, delayMs, confirmation.responseCode());
        return new GatewayCheckoutResult(processId, checkoutScriptUrl());
    }

    @Override
    public Optional<GatewayConfirmation> getConfirmation(Payment payment) {
        // El mock siempre "resuelve" según las reglas determinísticas, igual que
        // get_single_buy_confirmation devolvería la transacción ya procesada.
        return Optional.of(buildConfirmation(payment));
    }

    @Override
    public GatewayRollbackResult rollback(Payment payment) {
        log.info("[MOCK vPOS] single_buy_rollback pago id={} → RollbackSuccessful", payment.getId());
        return new GatewayRollbackResult(true, "RollbackSuccessful");
    }

    /** Arma la confirmación con el mismo shape y token que enviaría Bancard. */
    GatewayConfirmation buildConfirmation(Payment payment) {
        String shopProcessId = String.valueOf(payment.getId());
        String amount = BancardTokens.formatAmount(payment.getAmount());
        String responseCode = responseCodeFor(payment.getAmount());
        boolean approved = "00".equals(responseCode);
        String token = BancardTokens.confirmToken(
                properties.getBancard().getPrivateKey(), shopProcessId, amount, CURRENCY);
        return new GatewayConfirmation(
                shopProcessId,
                approved ? "S" : "N",
                responseCode,
                describeResponseCode(responseCode),
                approved ? String.format("%06d", payment.getId() % 1_000_000) : null,
                approved ? String.valueOf(100_000_000L + payment.getId()) : null,
                amount,
                CURRENCY,
                token);
    }

    /** Reglas determinísticas sobre los dos últimos dígitos de la parte entera. */
    static String responseCodeFor(BigDecimal amount) {
        int lastTwoDigits = amount.toBigInteger().mod(java.math.BigInteger.valueOf(100)).intValue();
        return switch (lastTwoDigits) {
            case 99 -> "51";
            case 15 -> "15";
            default -> "00";
        };
    }

    static String describeResponseCode(String code) {
        return switch (code) {
            case "00" -> "Transaccion aprobada";
            case "05" -> "Tarjeta inhabilitada";
            case "12" -> "Transaccion invalida";
            case "15" -> "Tarjeta invalida";
            case "51" -> "Fondos insuficientes";
            default -> "Transaccion rechazada";
        };
    }

    /** Body idéntico al POST single_buy_confirm que Bancard envía a la confirmation_url. */
    Map<String, Object> buildConfirmationBody(GatewayConfirmation confirmation) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("token", confirmation.token());
        operation.put("shop_process_id", confirmation.shopProcessId());
        operation.put("response", confirmation.response());
        operation.put("response_details", confirmation.responseDescription());
        operation.put("amount", confirmation.amount());
        operation.put("currency", confirmation.currency());
        operation.put("authorization_number", confirmation.authorizationNumber());
        operation.put("ticket_number", confirmation.ticketNumber());
        operation.put("response_code", confirmation.responseCode());
        operation.put("response_description", confirmation.responseDescription());
        operation.put("extended_response_description", confirmation.responseDescription());
        operation.put("security_information", Map.of(
                "customer_ip", "127.0.0.1",
                "card_source", "L",
                "card_country", "Paraguay",
                "version", "0.3",
                "risk_index", "1"));
        return Map.of("operation", operation);
    }

    void postConfirmation(GatewayConfirmation confirmation) {
        try {
            restClient.post()
                    .uri(properties.getMock().getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildConfirmationBody(confirmation))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[MOCK vPOS] single_buy_confirm enviado para shop_process_id={} (response_code={})",
                    confirmation.shopProcessId(), confirmation.responseCode());
        } catch (Exception e) {
            log.error("[MOCK vPOS] fallo al invocar el webhook propio en {}: {}",
                    properties.getMock().getWebhookUrl(), e.getMessage());
        }
    }

    private String checkoutScriptUrl() {
        return properties.getBancard().getBaseUrl() + "/checkout/javascript/dist/bancard-checkout-4.0.0.js";
    }
}
