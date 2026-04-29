package com.openroof.openroof.exception;

/**
 * Se lanza cuando no se pudo obtener una cotización válida y no existe cache de respaldo.
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }

    public ExchangeRateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
