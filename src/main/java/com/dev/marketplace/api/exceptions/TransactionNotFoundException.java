package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando se busca una transacción por su id y no existe en la base de
 * datos. Se traduce a un 404 Not Found mediante {@link GlobalExceptionHandler}.
 */
public class TransactionNotFoundException extends RuntimeException {

    /**
     * Crea la excepción indicando el id de la transacción no encontrada.
     *
     * @param id identificador de la transacción buscada
     */
    public TransactionNotFoundException(String id) {
        super("Transaccion no encontrada " + id);
    }
}
