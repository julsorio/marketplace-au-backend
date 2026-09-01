package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando un usuario intenta acceder a una conversación de la que no es
 * participante. Se traduce a un 403 Forbidden mediante {@link GlobalExceptionHandler}.
 */
public class UnauthorizedConversationAccessException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje por defecto de falta de acceso a la conversación.
     */
    public UnauthorizedConversationAccessException() {
        super("No tienes acceso a esta conversacion");
    }
}
