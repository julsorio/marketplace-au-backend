package com.dev.marketplace.api.exceptions;

/**
 * UnauthorizedListingAccessException
 */
public class UnauthorizedListingAccessException extends RuntimeException {
    public UnauthorizedListingAccessException() {
        super("No tienes permiso para modificar este anuncio");
    }
}
