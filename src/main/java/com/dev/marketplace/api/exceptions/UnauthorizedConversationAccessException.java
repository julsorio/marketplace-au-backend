package com.dev.marketplace.api.exceptions;

public class UnauthorizedConversationAccessException extends RuntimeException {
    public UnauthorizedConversationAccessException() {
        super("No tienes acceso a esta conversacion");
    }
}
