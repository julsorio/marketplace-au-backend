package com.dev.marketplace.api.exceptions;

/**
 * ListingNotFoundException
 */
public class ListingNotFoundException extends RuntimeException {
    public ListingNotFoundException(String id) {
        super("Anuncio no encontrado " + id);
    }
}
