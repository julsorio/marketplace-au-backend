package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando se busca un usuario por su id (u otro identificador) y no existe
 * en la base de datos. Se traduce a un 404 Not Found mediante {@link GlobalExceptionHandler}.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Crea la excepción indicando el id del usuario no encontrado.
     *
     * @param id identificador del usuario buscado
     */
    public UserNotFoundException(String id) {
        super("Usuario no encontrado " + id);
    }
}
