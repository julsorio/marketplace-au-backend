package com.dev.marketplace.api.response.dto;

import java.time.Instant;

/**
 * Datos de una reseña devueltos por la API.
 *
 * @param id identificador de la reseña
 * @param listingId id del anuncio/transacción al que está ligada la reseña
 * @param reviewerId id del usuario que escribió la reseña
 * @param revieweeId id del usuario reseñado
 * @param rating puntuación de la reseña, de 1 a 5
 * @param comment comentario de la reseña
 * @param createdAt fecha de creación de la reseña
 */
public record ReviewResponse(String id,
        String listingId,
        String reviewerId,
        String revieweeId,
        int rating,
        String comment,
        Instant createdAt) {
}
