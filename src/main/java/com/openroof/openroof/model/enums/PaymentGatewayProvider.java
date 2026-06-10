package com.openroof.openroof.model.enums;

/**
 * Pasarela de pagos seleccionable por configuración (clave {@code payments.gateway}).
 * No confundir con {@link PaymentGatewayType}, reservado para lease_payments.
 */
public enum PaymentGatewayProvider {
    MOCK,
    BANCARD
}
