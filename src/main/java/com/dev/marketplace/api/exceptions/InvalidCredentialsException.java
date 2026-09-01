package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando el email o la contraseña proporcionados en el login no son
 * correctos. Se traduce a un 401 Unauthorized mediante {@link GlobalExceptionHandler},
 * igual que {@link org.springframework.security.authentication.BadCredentialsException}.
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje por defecto de credenciales incorrectas.
     */
    public InvalidCredentialsException() {
        super("Email o contrasena incorrectos");
    }
}
