package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando un usuario intenta operar sobre una transacción en la que no
 * participa (ni como comprador ni como vendedor). Se traduce a un 403 Forbidden mediante
 * {@link GlobalExceptionHandler}.
 */
public class UnauthorizedTransactionAccessException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje por defecto de falta de permiso sobre la transacción.
     */
    public UnauthorizedTransactionAccessException() {
        super("No tienes permiso para esta operación sobre la transacción");
    }

}
