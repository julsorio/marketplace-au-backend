package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando un usuario intenta modificar o eliminar un listing del que no
 * es propietario. Se traduce a un 403 Forbidden mediante {@link GlobalExceptionHandler}.
 */
public class UnauthorizedListingAccessException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje por defecto de falta de permiso sobre el anuncio.
     */
    public UnauthorizedListingAccessException() {
        super("No tienes permiso para modificar este anuncio");
    }
}
