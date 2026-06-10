package com.openroof.openroof.service;

import com.openroof.openroof.config.PaymentGatewayProperties;
import com.openroof.openroof.dto.payment.BancardConfirmOperation;
import com.openroof.openroof.dto.payment.CheckoutRequest;
import com.openroof.openroof.dto.payment.CheckoutResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.gateway.BancardTokens;
import com.openroof.openroof.gateway.GatewayCheckoutResult;
import com.openroof.openroof.gateway.GatewayConfirmation;
import com.openroof.openroof.gateway.GatewayRollbackResult;
import com.openroof.openroof.gateway.PaymentGateway;
import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    private static final String PRIVATE_KEY = "test-private-key";

    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;
    @Mock private PaymentGateway paymentGateway;

    private PaymentGatewayProperties properties;
    private PaymentGatewayService service;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        properties.getBancard().setPrivateKey(PRIVATE_KEY);
        service = new PaymentGatewayService(
                paymentRepository, userRepository, paymentService, paymentGateway, properties);

        owner = User.builder().name("Juan Pérez").email("user@test.com").role(UserRole.USER).build();
        owner.setId(1L);
        other = User.builder().name("Otra Persona").email("other@test.com").role(UserRole.USER).build();
        other.setId(2L);

        lenient().when(paymentGateway.provider()).thenReturn(PaymentGatewayProvider.MOCK);
    }

    private Payment buildPayment(Long id, PaymentStatus status) {
        Payment p = Payment.builder()
                .user(owner)
                .type(PaymentType.RESERVATION)
                .status(status)
                .concept("Señal de reserva")
                .amount(new BigDecimal("350000.00"))
                .transactionCode("uuid-" + id)
                .build();
        p.setId(id);
        return p;
    }

    private BancardConfirmOperation confirmOperation(String shopProcessId, String amount,
                                                     String responseCode, String token) {
        boolean approved = "00".equals(responseCode);
        return new BancardConfirmOperation(
                token,
                shopProcessId,
                approved ? "S" : "N",
                "detalle",
                amount,
                "PYG",
                approved ? "123456" : null,
                approved ? "100000873" : null,
                responseCode,
                approved ? "Transaccion aprobada" : "Fondos insuficientes",
                null);
    }

    private String validToken(String shopProcessId, String amount) {
        return BancardTokens.confirmToken(PRIVATE_KEY, shopProcessId, amount, "PYG");
    }

    // ─── Checkout ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkout()")
    class Checkout {

        @Test
        @DisplayName("Dueño con pago PENDING inicia checkout: guarda process_id y checkout_started_at")
        void ownerStartsCheckout() {
            Payment payment = buildPayment(873L, PaymentStatus.PENDING);
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
            when(paymentGateway.createCheckout(eq(payment), anyString(), anyString()))
                    .thenReturn(new GatewayCheckoutResult("proc-123", "https://script.js"));

            CheckoutResponse response = service.checkout(873L, "user@test.com", null);

            assertThat(response.processId()).isEqualTo("proc-123");
            assertThat(response.checkoutScriptUrl()).isEqualTo("https://script.js");
            assertThat(response.gateway()).isEqualTo(PaymentGatewayProvider.MOCK);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getGatewayProcessId()).isEqualTo("proc-123");
            assertThat(captor.getValue().getGateway()).isEqualTo(PaymentGatewayProvider.MOCK);
            assertThat(captor.getValue().getCheckoutStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Usa las URLs del request cuando vienen en el body")
        void usesRequestUrls() {
            Payment payment = buildPayment(873L, PaymentStatus.PENDING);
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
            when(paymentGateway.createCheckout(any(), anyString(), anyString()))
                    .thenReturn(new GatewayCheckoutResult("proc-123", "https://script.js"));

            service.checkout(873L, "user@test.com",
                    new CheckoutRequest("https://app/return", "https://app/cancel"));

            verify(paymentGateway).createCheckout(payment, "https://app/return", "https://app/cancel");
        }

        @Test
        @DisplayName("Otro usuario lanza ForbiddenException")
        void otherUserForbidden() {
            Payment payment = buildPayment(873L, PaymentStatus.PENDING);
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.checkout(873L, "other@test.com", null))
                    .isInstanceOf(ForbiddenException.class);
            verifyNoInteractions(paymentGateway);
        }

        @Test
        @DisplayName("Pago no PENDING lanza BadRequestException")
        void nonPendingRejected() {
            Payment payment = buildPayment(873L, PaymentStatus.APPROVED);
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> service.checkout(873L, "user@test.com", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING");
            verifyNoInteractions(paymentGateway);
        }

        @Test
        @DisplayName("Checkout repetido devuelve el process_id existente sin llamar a la pasarela")
        void repeatedCheckoutReturnsExistingProcessId() {
            Payment payment = buildPayment(873L, PaymentStatus.PENDING);
            payment.setGateway(PaymentGatewayProvider.MOCK);
            payment.setGatewayProcessId("proc-existing");
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));

            CheckoutResponse response = service.checkout(873L, "user@test.com", null);

            assertThat(response.processId()).isEqualTo("proc-existing");
            verifyNoInteractions(paymentGateway);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Pago inexistente lanza ResourceNotFoundException")
        void paymentNotFound() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkout(999L, "user@test.com", null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Webhook ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processWebhookConfirmation()")
    class Webhook {

        @Test
        @DisplayName("Aprobado (00): verifica token, guarda auth/ticket y reusa approve + complete")
        void approvedConfirmation() {
            Payment payment = buildPayment(873L, PaymentStatus.PENDING);
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));

            boolean processed = service.processWebhookConfirmation(
                    confirmOperation("873", "350000.00", "00", validToken("873", "350000.00")));

            assertThat(processed).isTrue();
            verify(paymentService).approvePayment(873L);
            verify(paymentService).completePayment(873L);
            verify(paymentService, never()).rejectPayment(any());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getGatewayResponseCode()).isEqualTo("00");
            assertThat(captor.getValue().getGatewayAuthorizationNumber()).isEqualTo("123456");
            assertThat(captor.getValue().getGatewayTicketNumber()).isEqualTo("100000873");
        }

        @Test
        @DisplayName("Rechazado (51): marca REJECTED sin efectos secundarios")
        void rejectedConfirmation() {
            Payment payment = buildPayment(874L, PaymentStatus.PENDING);
            when(paymentRepository.findById(874L)).thenReturn(Optional.of(payment));

            boolean processed = service.processWebhookConfirmation(
                    confirmOperation("874", "150099.00", "51", validToken("874", "150099.00")));

            assertThat(processed).isTrue();
            verify(paymentService).rejectPayment(874L);
            verify(paymentService, never()).approvePayment(any());
            verify(paymentService, never()).completePayment(any());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getGatewayResponseCode()).isEqualTo("51");
        }

        @Test
        @DisplayName("Token inválido: no procesa nada")
        void invalidToken() {
            boolean processed = service.processWebhookConfirmation(
                    confirmOperation("873", "350000.00", "00", "token-falsificado"));

            assertThat(processed).isFalse();
            verifyNoInteractions(paymentService);
            verifyNoInteractions(paymentRepository);
        }

        @Test
        @DisplayName("Retry idempotente: pago ya resuelto devuelve true sin reprocesar")
        void idempotentRetry() {
            Payment payment = buildPayment(873L, PaymentStatus.COMPLETED);
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));

            boolean processed = service.processWebhookConfirmation(
                    confirmOperation("873", "350000.00", "00", validToken("873", "350000.00")));

            assertThat(processed).isTrue();
            verifyNoInteractions(paymentService);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Pago inexistente: devuelve false sin lanzar excepción")
        void unknownPayment() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            boolean processed = service.processWebhookConfirmation(
                    confirmOperation("999", "350000.00", "00", validToken("999", "350000.00")));

            assertThat(processed).isFalse();
            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("Payload incompleto (sin shop_process_id): se ignora")
        void incompletePayload() {
            assertThat(service.processWebhookConfirmation(null)).isFalse();
            assertThat(service.processWebhookConfirmation(
                    confirmOperation(null, "350000.00", "00", "x"))).isFalse();
            verifyNoInteractions(paymentService);
        }
    }

    // ─── Reconciliación ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reconcilePendingPayments()")
    class Reconciliation {

        @Test
        @DisplayName("Con confirmación de la pasarela: la aplica (approve + complete)")
        void appliesConfirmationWhenPresent() {
            Payment payment = buildPayment(873L, PaymentStatus.PENDING);
            payment.setGatewayProcessId("proc-1");
            payment.setCheckoutStartedAt(LocalDateTime.now().minusMinutes(20));
            when(paymentRepository.findByStatusAndGatewayProcessIdIsNotNullAndCheckoutStartedAtBefore(
                    eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(payment));
            when(paymentRepository.findById(873L)).thenReturn(Optional.of(payment));
            when(paymentGateway.getConfirmation(payment)).thenReturn(Optional.of(new GatewayConfirmation(
                    "873", "S", "00", "Transaccion aprobada",
                    "123456", "100000873", "350000.00", "PYG", "token")));

            service.reconcilePendingPayments();

            verify(paymentService).approvePayment(873L);
            verify(paymentService).completePayment(873L);
            verify(paymentGateway, never()).rollback(any());
        }

        @Test
        @DisplayName("Sin confirmación: reversa con rollback y marca REJECTED")
        void rollsBackWhenNoConfirmation() {
            Payment payment = buildPayment(874L, PaymentStatus.PENDING);
            payment.setGatewayProcessId("proc-2");
            payment.setCheckoutStartedAt(LocalDateTime.now().minusMinutes(20));
            when(paymentRepository.findByStatusAndGatewayProcessIdIsNotNullAndCheckoutStartedAtBefore(
                    eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(payment));
            when(paymentGateway.getConfirmation(payment)).thenReturn(Optional.empty());
            when(paymentGateway.rollback(payment))
                    .thenReturn(new GatewayRollbackResult(true, "PaymentNotFoundError"));

            service.reconcilePendingPayments();

            verify(paymentService).rejectPayment(874L);
            verify(paymentService, never()).approvePayment(any());
        }

        @Test
        @DisplayName("Rollback no aplicado: deja el pago PENDING para el próximo ciclo")
        void keepsPendingWhenRollbackFails() {
            Payment payment = buildPayment(875L, PaymentStatus.PENDING);
            payment.setGatewayProcessId("proc-3");
            payment.setCheckoutStartedAt(LocalDateTime.now().minusMinutes(20));
            when(paymentRepository.findByStatusAndGatewayProcessIdIsNotNullAndCheckoutStartedAtBefore(
                    eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(payment));
            when(paymentGateway.getConfirmation(payment)).thenReturn(Optional.empty());
            when(paymentGateway.rollback(payment))
                    .thenReturn(new GatewayRollbackResult(false, "TransactionAlreadyConfirmed"));

            service.reconcilePendingPayments();

            verify(paymentService, never()).rejectPayment(any());
        }

        @Test
        @DisplayName("Error en un pago no corta la reconciliación de los demás")
        void continuesOnError() {
            Payment failing = buildPayment(876L, PaymentStatus.PENDING);
            Payment ok = buildPayment(877L, PaymentStatus.PENDING);
            when(paymentRepository.findByStatusAndGatewayProcessIdIsNotNullAndCheckoutStartedAtBefore(
                    eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(failing, ok));
            when(paymentGateway.getConfirmation(failing)).thenThrow(new RuntimeException("timeout"));
            when(paymentGateway.getConfirmation(ok)).thenReturn(Optional.empty());
            when(paymentGateway.rollback(ok)).thenReturn(new GatewayRollbackResult(true, "RollbackSuccessful"));

            service.reconcilePendingPayments();

            verify(paymentService).rejectPayment(877L);
        }
    }
}
