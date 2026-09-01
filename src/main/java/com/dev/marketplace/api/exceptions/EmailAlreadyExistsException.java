package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada al registrar un usuario con un email que ya está en uso por otra cuenta.
 * Se traduce a un 409 Conflict mediante {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Crea la excepción indicando el email duplicado.
     *
     * @param email dirección de email que ya está registrada
     */
    public EmailAlreadyExistsException(String email) {
        super("El email " + email + " ya esta registrado");
    }

}
