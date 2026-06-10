package com.openroof.openroof.gateway;

/**
 * Resultado de un single_buy_rollback. {@code rolledBack} es true cuando la reversa
 * fue aceptada o cuando Bancard responde PaymentNotFoundError (el cliente nunca pagó,
 * la spec indica tratarlo como respuesta correcta) o AlreadyRollbackedError.
 */
public record GatewayRollbackResult(
        boolean rolledBack,
        String key
) {}
