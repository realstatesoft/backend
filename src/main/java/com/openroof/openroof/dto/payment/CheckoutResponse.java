package com.openroof.openroof.dto.payment;

import com.openroof.openroof.model.enums.PaymentGatewayProvider;

/**
 * Respuesta del checkout: el process_id para levantar el iframe de pago con
 * bancard-checkout-js y la URL del script del ambiente activo.
 */
public record CheckoutResponse(
        String processId,
        String checkoutScriptUrl,
        PaymentGatewayProvider gateway
) {}
