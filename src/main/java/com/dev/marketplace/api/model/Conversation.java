package com.dev.marketplace.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Documento MongoDB que representa una conversación de chat entre dos usuarios (comprador
 * y vendedor) en torno a un listing concreto. Guarda un resumen del último mensaje y el
 * contador de mensajes no leídos por usuario, para poder listar conversaciones sin tener
 * que consultar la colección de mensajes.
 */
@Data
@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;

    /**
     * Identificador del listing sobre el que trata la conversación.
     * Indexado para poder buscar conversaciones por listing.
     */
    @Indexed
    private String listingId;

    /**
     * Identificadores de los dos usuarios que participan en la conversación:
     * {@code [buyerId, sellerId]}. Indexado para poder listar las conversaciones de un usuario.
     */
    @Indexed
    private List<String> participants;

    /**
     * Resumen embebido del último mensaje enviado en la conversación (texto, fecha de envío
     * y remitente), para poder listar conversaciones sin consultar la colección de mensajes.
     */
    private LastMessage lastMessage;

    /**
     * Contador de mensajes no leídos por usuario: {@code { userId: cantidad de no leídos } }.
     */
    private Map<String, Integer> unreadCount;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
