package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando se busca una conversación por su id y no existe en la base de datos.
 * Se traduce a un 404 Not Found mediante {@link GlobalExceptionHandler}.
 */
public class ConversationNotFoundException extends RuntimeException {

    /**
     * Crea la excepción indicando el id de la conversación no encontrada.
     *
     * @param id identificador de la conversación buscada
     */
    public ConversationNotFoundException(String id) {
        super("Conversacion no encontrada " + id);
    }
}
