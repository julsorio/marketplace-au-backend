package com.dev.marketplace.api.response.dto;

import java.time.Instant;

/**
 * Datos de una conversación devueltos al cliente, incluyendo un resumen de su último
 * mensaje y el contador de mensajes no leídos para el usuario que consulta.
 *
 * @param id                identificador de la conversación
 * @param listingId         identificador del listing sobre el que trata la conversación
 * @param participants      identificadores de los usuarios que participan en la conversación
 * @param lastMessageText   texto del último mensaje enviado, o {@code null} si aún no hay mensajes
 * @param lastMessageSentAt fecha y hora del último mensaje enviado, o {@code null} si aún no hay mensajes
 * @param unreadCount       cantidad de mensajes no leídos para el usuario que consulta
 */
public record ConversationResponse(String id,
        String listingId,
        java.util.List<String> participants,
        String lastMessageText,
        Instant lastMessageSentAt,
        int unreadCount) {
}
