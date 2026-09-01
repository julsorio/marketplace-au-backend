package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando se busca un listing (anuncio) por su id y no existe en la base
 * de datos. Se traduce a un 404 Not Found mediante {@link GlobalExceptionHandler}.
 */
public class ListingNotFoundException extends RuntimeException {

    /**
     * Crea la excepción indicando el id del listing no encontrado.
     *
     * @param id identificador del listing buscado
     */
    public ListingNotFoundException(String id) {
        super("Anuncio no encontrado " + id);
    }
}
