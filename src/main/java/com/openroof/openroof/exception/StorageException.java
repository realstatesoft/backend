package com.openroof.openroof.exception;

/**
 * Excepción lanzada cuando ocurre un error al interactuar con el servicio de almacenamiento.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
