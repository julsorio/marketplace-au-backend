package com.dev.marketplace.api.response.dto;

/**
 * Datos de un mensaje de chat devueltos al cliente.
 *
 * @param id             identificador del mensaje
 * @param conversationId identificador de la conversación a la que pertenece el mensaje
 * @param senderId       identificador del usuario que envió el mensaje
 * @param text           contenido del mensaje
 * @param sentAt         fecha y hora en que se envió el mensaje
 * @param read           {@code true} si el mensaje ya ha sido leído por el destinatario
 */
public record MessageResponse(String id,
        String conversationId,
        String senderId,
        String text,
        java.time.Instant sentAt,
        boolean read) {
}
