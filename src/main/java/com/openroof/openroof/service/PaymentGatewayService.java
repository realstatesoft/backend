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
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PaymentRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta el flujo de pasarela sobre el módulo Payments: checkout, procesamiento
 * de confirmaciones (webhook + reconciliación) y reversas. El flujo manual
 * admin (approve/reject) sigue intacto para pagos sin gateway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final PaymentGatewayProperties properties;

    // ─── Checkout ──────────────────────────────────────────────────────────────

    /**
     * Inicia el checkout de un pago contra la pasarela activa.
     * Solo el dueño del pago, solo si está PENDING. Si el checkout ya fue
     * iniciado, devuelve el process_id existente sin volver a llamar a la
     * pasarela (Bancard rechaza shop_process_id repetidos).
     */
    @Transactional
    public CheckoutResponse checkout(Long paymentId, String currentUserEmail, CheckoutRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permiso para iniciar el checkout de este pago");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Solo se puede iniciar el checkout de pagos en estado PENDING");
        }
        if (payment.getGatewayProcessId() != null) {
            log.info("Checkout ya iniciado para pago id={}, devolviendo process_id existente", paymentId);
            return new CheckoutResponse(payment.getGatewayProcessId(), checkoutScriptUrl(), payment.getGateway());
        }

        String returnUrl = (request != null && request.returnUrl() != null && !request.returnUrl().isBlank())
                ? request.returnUrl() : properties.getCheckout().getReturnUrl();
        String cancelUrl = (request != null && request.cancelUrl() != null && !request.cancelUrl().isBlank())
                ? request.cancelUrl() : properties.getCheckout().getCancelUrl();

        GatewayCheckoutResult result = paymentGateway.createCheckout(payment, returnUrl, cancelUrl);

        payment.setGateway(paymentGateway.provider());
        payment.setGatewayProcessId(result.processId());
        payment.setCheckoutStartedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return new CheckoutResponse(result.processId(), result.checkoutScriptUrl(), paymentGateway.provider());
    }

    // ─── Webhook single_buy_confirm ────────────────────────────────────────────

    /**
     * Procesa la confirmación recibida en el webhook. Verifica el token
     * md5(private_key + shop_process_id + "confirm" + amount + currency) con los
     * valores crudos del payload (los mismos que usó Bancard para generarlo).
     *
     * @return true si la confirmación fue aplicada o ya estaba aplicada (retry
     *         idempotente); false si el token es inválido o el pago no existe.
     */
    public boolean processWebhookConfirmation(BancardConfirmOperation operation) {
        if (operation == null || operation.shopProcessId() == null || operation.token() == null) {
            log.warn("Webhook Bancard con payload incompleto, se ignora");
            return false;
        }
        String expectedToken = BancardTokens.confirmToken(
                properties.getBancard().getPrivateKey(),
                operation.shopProcessId(),
                operation.amount(),
                operation.currency());
        if (!expectedToken.equalsIgnoreCase(operation.token())) {
            log.warn("Webhook Bancard con token inválido para shop_process_id={}, se ignora",
                    operation.shopProcessId());
            return false;
        }
        return applyConfirmation(new GatewayConfirmation(
                operation.shopProcessId(),
                operation.response(),
                operation.responseCode(),
                operation.responseDescription(),
                operation.authorizationNumber(),
                operation.ticketNumber(),
                operation.amount(),
                operation.currency(),
                operation.token()));
    }

    /**
     * Aplica una confirmación ya verificada (webhook) o recibida por canal directo
     * (get_single_buy_confirmation). Reusa la lógica de efectos secundarios de
     * approvePayment (highlight/suscripción) sin pasar por el endpoint admin.
     */
    @Transactional
    public boolean applyConfirmation(GatewayConfirmation confirmation) {
        Long paymentId = parsePaymentId(confirmation.shopProcessId());
        if (paymentId == null) {
            log.warn("Confirmación con shop_process_id no numérico: {}", confirmation.shopProcessId());
            return false;
        }
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Confirmación para pago inexistente id={}, se ignora", paymentId);
            return false;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Confirmación repetida para pago id={} (estado {}), retry idempotente",
                    paymentId, payment.getStatus());
            return true;
        }

        payment.setGatewayResponseCode(confirmation.responseCode());
        payment.setGatewayAuthorizationNumber(confirmation.authorizationNumber());
        payment.setGatewayTicketNumber(confirmation.ticketNumber());
        if (payment.getGateway() == null) {
            payment.setGateway(paymentGateway.provider());
        }
        paymentRepository.save(payment);

        if (confirmation.isApproved()) {
            // Efectos secundarios (highlight/suscripción) + transición PENDING→APPROVED→COMPLETED
            paymentService.approvePayment(paymentId);
            paymentService.completePayment(paymentId);
            log.info("Pago id={} confirmado por pasarela: APPROVED→COMPLETED (auth={}, ticket={})",
                    paymentId, confirmation.authorizationNumber(), confirmation.ticketNumber());
        } else {
            paymentService.rejectPayment(paymentId);
            log.info("Pago id={} rechazado por pasarela: response_code={} ({})",
                    paymentId, confirmation.responseCode(), confirmation.responseDescription());
        }
        return true;
    }

    // ─── Reconciliación ────────────────────────────────────────────────────────

    /**
     * Red de seguridad obligatoria de la spec: pagos PENDING con checkout iniciado
     * hace más de N minutos (default 10) se consultan vía get_single_buy_confirmation;
     * si la pasarela no registra confirmación, se reversa con single_buy_rollback
     * y el pago queda REJECTED.
     */
    public void reconcilePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(properties.getReconciliation().getStaleAfterMinutes());
        List<Payment> stale = paymentRepository
                .findByStatusAndGatewayProcessIdIsNotNullAndCheckoutStartedAtBefore(PaymentStatus.PENDING, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        log.info("Reconciliación de pasarela: {} pago(s) PENDING sin confirmación", stale.size());
        for (Payment payment : stale) {
            try {
                reconcile(payment);
            } catch (Exception e) {
                log.error("Error reconciliando pago id={}: {}", payment.getId(), e.getMessage());
            }
        }
    }

    private void reconcile(Payment payment) {
        Optional<GatewayConfirmation> confirmation = paymentGateway.getConfirmation(payment);
        if (confirmation.isPresent()) {
            applyConfirmation(confirmation.get());
            return;
        }
        GatewayRollbackResult rollback = paymentGateway.rollback(payment);
        if (rollback.rolledBack()) {
            paymentService.rejectPayment(payment.getId());
            log.info("Pago id={} reversado por reconciliación ({}) → REJECTED",
                    payment.getId(), rollback.key());
        } else {
            log.warn("Rollback no aplicado para pago id={} (key={}), se reintentará en el próximo ciclo",
                    payment.getId(), rollback.key());
        }
    }

    private Long parsePaymentId(String shopProcessId) {
        try {
            return Long.valueOf(shopProcessId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String checkoutScriptUrl() {
        return properties.getBancard().getBaseUrl() + "/checkout/javascript/dist/bancard-checkout-4.0.0.js";
    }
}
