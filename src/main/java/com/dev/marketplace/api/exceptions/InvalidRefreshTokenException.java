package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando el refresh token proporcionado no es válido, ha expirado o no
 * corresponde a ninguna sesión activa. Se traduce a un 401 Unauthorized mediante
 * {@link GlobalExceptionHandler}.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje por defecto de refresh token inválido o expirado.
     */
    public InvalidRefreshTokenException() {
        super("Refresh token inválido o expirado");
    }
}
