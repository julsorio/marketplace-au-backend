package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando un usuario intenta dejar una reseña que no cumple las reglas de
 * negocio para poder hacerlo (por ejemplo, la transacción no está en un estado que permita
 * reseñarla, o el usuario no participó en ella). Se traduce a un 400 Bad Request mediante
 * {@link GlobalExceptionHandler}.
 */
public class ReviewNotAllowedException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje describiendo por qué no se permite la reseña.
     *
     * @param message mensaje explicando la regla de negocio incumplida
     */
    public ReviewNotAllowedException(String message) {
        super(message);
    }

}
