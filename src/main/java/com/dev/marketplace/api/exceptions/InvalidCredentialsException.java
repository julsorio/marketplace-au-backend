package com.dev.marketplace.api.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email o contrasena incorrectos");
    }
}
