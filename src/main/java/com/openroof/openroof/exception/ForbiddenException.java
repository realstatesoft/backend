package com.openroof.openroof.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lanzada cuando un usuario autenticado intenta realizar una operación
 * sobre un recurso que no le pertenece o para el que no tiene el rol necesario.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
