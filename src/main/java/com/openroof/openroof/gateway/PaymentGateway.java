package com.openroof.openroof.gateway;

import com.openroof.openroof.model.enums.PaymentGatewayProvider;
import com.openroof.openroof.model.payment.Payment;

import java.util.Optional;

/**
 * Abstracción de pasarela de pagos (contrato Bancard vPOS 2.0 / Compra Simple).
 * La implementación activa se selecciona por configuración: {@code payments.gateway}.
 */
public interface PaymentGateway {

    PaymentGatewayProvider provider();

    /**
     * Inicia el proceso de pago (single_buy). Usa el id del Payment como shop_process_id.
     */
    GatewayCheckoutResult createCheckout(Payment payment, String returnUrl, String cancelUrl);

    /**
     * Consulta si existe confirmación para el pago (get_single_buy_confirmation).
     * Vacío si la pasarela aún no registra un pago confirmado/rechazado.
     */
    Optional<GatewayConfirmation> getConfirmation(Payment payment);

    /**
     * Reversa la transacción (single_buy_rollback).
     */
    GatewayRollbackResult rollback(Payment payment);
}
