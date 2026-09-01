package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos necesarios para enviar un mensaje de chat. Si es el primer mensaje entre el
 * remitente y el destinatario para el listing indicado, se crea automáticamente una
 * nueva conversación antes de guardar el mensaje.
 *
 * @param listingId   identificador del listing sobre el que trata la conversación
 * @param recipientId identificador del usuario destinatario del mensaje
 * @param text        contenido del mensaje
 */
public record SendMessageRequest(@NotBlank String listingId,
        @NotBlank String recipientId,
        @NotBlank String text) {
}
