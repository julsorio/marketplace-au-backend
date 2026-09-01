package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos necesarios para crear una reseña sobre otro usuario, ligada a un listing.
 *
 * @param listingId id del anuncio/transacción sobre el que se basa la reseña
 * @param revieweeId id del usuario que recibe la reseña
 * @param rating puntuación de la reseña, entre 1 y 5
 * @param comment comentario de la reseña (opcional, máximo 500 caracteres)
 */
public record CreateReviewRequest(@NotBlank String listingId,
        @NotBlank String revieweeId,
        @Min(1) @Max(5) int rating,
        @Size(max = 500) String comment) {
}
