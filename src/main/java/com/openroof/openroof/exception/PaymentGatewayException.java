package com.openroof.openroof.exception;

/**
 * Error al comunicarse con la pasarela de pagos (Bancard vPOS).
 * Se mapea a 502 Bad Gateway en el GlobalExceptionHandler.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
