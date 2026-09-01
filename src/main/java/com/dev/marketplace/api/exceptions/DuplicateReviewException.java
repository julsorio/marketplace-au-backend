package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando un usuario intenta dejar una reseña sobre una transacción
 * para la que ya existe una reseña suya. Se traduce a un 409 Conflict mediante
 * {@link GlobalExceptionHandler}.
 */
public class DuplicateReviewException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje por defecto indicando que la reseña ya existe.
     */
    public DuplicateReviewException() {
        super("Ya has dejado una reseña para esta transacción");
    }
}
