package com.dev.marketplace.api.model;

import java.time.Instant;

/**
 * Resumen embebido del último mensaje de una {@link Conversation}, guardado dentro del
 * propio documento de la conversación para poder listar conversaciones (con su último
 * mensaje) sin tener que consultar la colección de mensajes.
 *
 * @param text     texto del último mensaje
 * @param sentAt   fecha y hora en que se envió el último mensaje
 * @param senderId identificador del usuario que envió el último mensaje
 */
public record LastMessage(String text, Instant sentAt, String senderId) {}
