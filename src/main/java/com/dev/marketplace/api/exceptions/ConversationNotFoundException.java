package com.dev.marketplace.api.exceptions;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(String id) {
        super("Conversacion no encontrada " + id);
    }
}
