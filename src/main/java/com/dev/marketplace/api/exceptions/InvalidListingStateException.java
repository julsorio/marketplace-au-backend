package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando se intenta realizar una operación sobre un listing que no es
 * válida para su estado actual (por ejemplo, marcar como vendido un listing ya vendido).
 * Se traduce a un 409 Conflict mediante {@link GlobalExceptionHandler}.
 */
public class InvalidListingStateException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje describiendo la transición de estado no permitida.
     *
     * @param message mensaje explicando por qué el estado actual no permite la operación
     */
    public InvalidListingStateException(String message) {
        super(message);
    }
}
