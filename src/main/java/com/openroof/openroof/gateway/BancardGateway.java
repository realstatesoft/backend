package com.openroof.openroof.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.openroof.openroof.config.PaymentGatewayProperties;
import com.openroof.openroof.exception.PaymentGatewayException;
import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import com.openroof.openroof.model.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cliente Bancard vPOS 2.0 — Compra Simple (single_buy) según spec v1.23.
 * shop_process_id = id del Payment · amount string con 2 decimales · currency PYG.
 */
@Slf4j
@RequiredArgsConstructor
public class BancardGateway implements PaymentGateway {

    private static final String CURRENCY = "PYG";
    private static final int MAX_DESCRIPTION_LENGTH = 20;

    /** Claves de rollback que la spec indica tratar como reversa efectiva. */
    private static final Set<String> ROLLBACK_OK_KEYS =
            Set.of("RollbackSuccessful", "PaymentNotFoundError", "AlreadyRollbackedError");

    private final PaymentGatewayProperties properties;
    private final RestClient restClient;

    @Override
    public PaymentGatewayProvider provider() {
        return PaymentGatewayProvider.BANCARD;
    }

    @Override
    public GatewayCheckoutResult createCheckout(Payment payment, String returnUrl, String cancelUrl) {
        String amount = BancardTokens.formatAmount(payment.getAmount());
        String token = BancardTokens.singleBuyToken(
                properties.getBancard().getPrivateKey(), payment.getId(), amount, CURRENCY);

        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("token", token);
        operation.put("shop_process_id", payment.getId());
        operation.put("amount", amount);
        operation.put("currency", CURRENCY);
        operation.put("description", truncate(payment.getConcept(), MAX_DESCRIPTION_LENGTH));
        operation.put("return_url", returnUrl);
        operation.put("cancel_url", cancelUrl);

        JsonNode response = post("/vpos/api/0.3/single_buy", Map.of(
                "public_key", properties.getBancard().getPublicKey(),
                "operation", operation));

        if (!"success".equals(response.path("status").asText())) {
            throw new PaymentGatewayException("Bancard rechazó el single_buy del pago "
                    + payment.getId() + ": " + response.toString());
        }
        String processId = response.path("process_id").asText();
        log.info("Bancard single_buy OK: pago id={} → process_id={}", payment.getId(), processId);
        return new GatewayCheckoutResult(processId, checkoutScriptUrl());
    }

    @Override
    public Optional<GatewayConfirmation> getConfirmation(Payment payment) {
        String token = BancardTokens.getConfirmationToken(
                properties.getBancard().getPrivateKey(), payment.getId());

        JsonNode response = post("/vpos/api/0.3/single_buy/confirmations", Map.of(
                "public_key", properties.getBancard().getPublicKey(),
                "operation", Map.of(
                        "token", token,
                        "shop_process_id", payment.getId())));

        JsonNode confirmation = response.path("confirmation");
        if (!"success".equals(response.path("status").asText()) || confirmation.isMissingNode()
                || confirmation.isNull()) {
            log.info("Bancard get_single_buy_confirmation sin confirmación para pago id={}: {}",
                    payment.getId(), response.path("messages").toString());
            return Optional.empty();
        }
        return Optional.of(new GatewayConfirmation(
                confirmation.path("shop_process_id").asText(),
                confirmation.path("response").asText(null),
                confirmation.path("response_code").asText(null),
                confirmation.path("response_description").asText(null),
                confirmation.path("authorization_number").asText(null),
                confirmation.path("ticket_number").asText(null),
                confirmation.path("amount").asText(null),
                confirmation.path("currency").asText(null),
                confirmation.path("token").asText(null)));
    }

    @Override
    public GatewayRollbackResult rollback(Payment payment) {
        String token = BancardTokens.rollbackToken(
                properties.getBancard().getPrivateKey(), payment.getId());

        JsonNode response = post("/vpos/api/0.3/single_buy/rollback", Map.of(
                "public_key", properties.getBancard().getPublicKey(),
                "operation", Map.of(
                        "token", token,
                        "shop_process_id", String.valueOf(payment.getId()))));

        String key = response.path("messages").path(0).path("key").asText("");
        boolean rolledBack = "success".equals(response.path("status").asText())
                || ROLLBACK_OK_KEYS.contains(key);
        log.info("Bancard single_buy_rollback pago id={} → status={} key={}",
                payment.getId(), response.path("status").asText(), key);
        return new GatewayRollbackResult(rolledBack, key);
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri(properties.getBancard().getBaseUrl() + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new PaymentGatewayException("Error de comunicación con Bancard en " + path, e);
        }
    }

    private String checkoutScriptUrl() {
        return properties.getBancard().getBaseUrl() + "/checkout/javascript/dist/bancard-checkout-4.0.0.js";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
