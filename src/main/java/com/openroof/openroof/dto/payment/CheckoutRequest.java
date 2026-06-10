package com.openroof.openroof.dto.payment;

import jakarta.validation.constraints.Size;

/**
 * Body opcional del checkout. Si no se envía, se usan los defaults de configuración
 * (payments.checkout.return-url / cancel-url).
 */
public record CheckoutRequest(
        @Size(max = 255, message = "return_url no puede exceder 255 caracteres")
        String returnUrl,

        @Size(max = 255, message = "cancel_url no puede exceder 255 caracteres")
        String cancelUrl
) {}
