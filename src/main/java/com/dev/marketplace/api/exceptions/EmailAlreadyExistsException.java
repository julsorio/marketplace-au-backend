package com.dev.marketplace.api.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("El email " + email + " ya esta registrado");
    }

}
