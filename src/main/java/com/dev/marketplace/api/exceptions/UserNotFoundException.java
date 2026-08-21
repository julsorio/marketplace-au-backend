package com.dev.marketplace.api.exceptions;

/**
 * UserNotFoundException
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String id) {
        super("Usuario no encontrado " + id);
    }
}
