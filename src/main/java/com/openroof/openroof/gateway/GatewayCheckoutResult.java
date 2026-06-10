package com.openroof.openroof.gateway;

/**
 * Resultado de iniciar un checkout en la pasarela: el process_id para levantar
 * el iframe de pago y la URL del script bancard-checkout-js correspondiente al ambiente.
 */
public record GatewayCheckoutResult(
        String processId,
        String checkoutScriptUrl
) {}
