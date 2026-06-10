package com.openroof.openroof.gateway;

import com.openroof.openroof.config.PaymentGatewayProperties;
import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MockPaymentGatewayTest {

    private static final String PRIVATE_KEY = "test-private-key";

    @Mock private TaskScheduler taskScheduler;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private RestClient restClient;

    private PaymentGatewayProperties properties;
    private MockPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        properties.getBancard().setPrivateKey(PRIVATE_KEY);
        gateway = new MockPaymentGateway(properties, taskScheduler, restClient);
    }

    private Payment buildPayment(Long id, String amount) {
        User user = User.builder().name("Juan").email("user@test.com").build();
        user.setId(1L);
        Payment p = Payment.builder()
                .user(user)
                .type(PaymentType.SUBSCRIPTION)
                .status(PaymentStatus.PENDING)
                .concept("Suscripción Plan Agente Pro")
                .amount(new BigDecimal(amount))
                .transactionCode("uuid-" + id)
                .build();
        p.setId(id);
        return p;
    }

    @Nested
    @DisplayName("Reglas determinísticas de response_code")
    class DeterministicRules {

        @Test
        @DisplayName("Monto terminado en 99 → \"51\" (fondos insuficientes)")
        void amountEndingIn99() {
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("150099"))).isEqualTo("51");
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("99.00"))).isEqualTo("51");
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("2500099.00"))).isEqualTo("51");
        }

        @Test
        @DisplayName("Monto terminado en 15 → \"15\" (tarjeta inválida)")
        void amountEndingIn15() {
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("350015"))).isEqualTo("15");
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("15.00"))).isEqualTo("15");
        }

        @Test
        @DisplayName("Resto → \"00\" (aprobada)")
        void otherAmounts() {
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("350000"))).isEqualTo("00");
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("1000000.00"))).isEqualTo("00");
            assertThat(MockPaymentGateway.responseCodeFor(new BigDecimal("123451"))).isEqualTo("00");
        }
    }

    @Nested
    @DisplayName("createCheckout()")
    class CreateCheckout {

        @Test
        @DisplayName("Devuelve process_id y URL del script de checkout, y agenda la confirmación")
        void returnsProcessIdAndSchedulesConfirmation() {
            GatewayCheckoutResult result = gateway.createCheckout(
                    buildPayment(873L, "350000"), "http://localhost/return", "http://localhost/cancel");

            assertThat(result.processId()).isNotBlank().startsWith("mock-873-");
            assertThat(result.checkoutScriptUrl())
                    .isEqualTo("https://vpos.infonet.com.py:8888/checkout/javascript/dist/bancard-checkout-4.0.0.js");
            verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("buildConfirmation() — contrato single_buy_confirm")
    class BuildConfirmation {

        @Test
        @DisplayName("Pago aprobado: response S, código 00, auth/ticket presentes y token md5 válido")
        void approvedConfirmation() {
            GatewayConfirmation c = gateway.buildConfirmation(buildPayment(873L, "350000"));

            assertThat(c.shopProcessId()).isEqualTo("873");
            assertThat(c.response()).isEqualTo("S");
            assertThat(c.responseCode()).isEqualTo("00");
            assertThat(c.isApproved()).isTrue();
            assertThat(c.amount()).isEqualTo("350000.00");
            assertThat(c.currency()).isEqualTo("PYG");
            assertThat(c.authorizationNumber()).isNotBlank();
            assertThat(c.ticketNumber()).isNotBlank();
            assertThat(c.token()).isEqualTo(
                    BancardTokens.confirmToken(PRIVATE_KEY, "873", "350000.00", "PYG"));
        }

        @Test
        @DisplayName("Monto ...99: response N, código 51, sin authorization_number")
        void insufficientFundsConfirmation() {
            GatewayConfirmation c = gateway.buildConfirmation(buildPayment(874L, "150099"));

            assertThat(c.response()).isEqualTo("N");
            assertThat(c.responseCode()).isEqualTo("51");
            assertThat(c.isApproved()).isFalse();
            assertThat(c.authorizationNumber()).isNull();
            assertThat(c.ticketNumber()).isNull();
            assertThat(c.token()).isEqualTo(
                    BancardTokens.confirmToken(PRIVATE_KEY, "874", "150099.00", "PYG"));
        }

        @Test
        @DisplayName("Monto ...15: código 15 (tarjeta inválida)")
        void invalidCardConfirmation() {
            GatewayConfirmation c = gateway.buildConfirmation(buildPayment(875L, "350015"));

            assertThat(c.response()).isEqualTo("N");
            assertThat(c.responseCode()).isEqualTo("15");
        }
    }

    @Nested
    @DisplayName("buildConfirmationBody() — JSON del webhook")
    class BuildConfirmationBody {

        @Test
        @DisplayName("Replica el shape exacto del POST single_buy_confirm de Bancard")
        void bodyMatchesBancardContract() {
            GatewayConfirmation c = gateway.buildConfirmation(buildPayment(873L, "350000"));
            Map<String, Object> body = gateway.buildConfirmationBody(c);

            assertThat(body).containsOnlyKeys("operation");
            @SuppressWarnings("unchecked")
            Map<String, Object> operation = (Map<String, Object>) body.get("operation");
            assertThat(operation)
                    .containsEntry("token", c.token())
                    .containsEntry("shop_process_id", "873")
                    .containsEntry("response", "S")
                    .containsEntry("amount", "350000.00")
                    .containsEntry("currency", "PYG")
                    .containsEntry("response_code", "00")
                    .containsKeys("authorization_number", "ticket_number",
                            "response_description", "security_information");
        }
    }

    @Test
    @DisplayName("getConfirmation() siempre resuelve según las reglas determinísticas")
    void getConfirmationResolves() {
        assertThat(gateway.getConfirmation(buildPayment(900L, "150099")))
                .hasValueSatisfying(c -> assertThat(c.responseCode()).isEqualTo("51"));
    }

    @Test
    @DisplayName("rollback() siempre es exitoso")
    void rollbackAlwaysSucceeds() {
        GatewayRollbackResult result = gateway.rollback(buildPayment(901L, "350000"));
        assertThat(result.rolledBack()).isTrue();
        assertThat(result.key()).isEqualTo("RollbackSuccessful");
    }

    @Test
    @DisplayName("provider() es MOCK")
    void providerIsMock() {
        assertThat(gateway.provider()).isEqualTo(PaymentGatewayProvider.MOCK);
    }
}
