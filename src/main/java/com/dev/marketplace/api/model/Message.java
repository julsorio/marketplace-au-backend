package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Documento MongoDB que representa un mensaje individual dentro de una conversación de chat.
 */
@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    /**
     * Identificador de la {@link Conversation} a la que pertenece este mensaje.
     * Indexado para poder listar los mensajes de una conversación.
     */
    @Indexed
    private String conversationId;

    private String senderId;
    private String text;

    private Instant sentAt = Instant.now();
    private boolean read = false;
}
