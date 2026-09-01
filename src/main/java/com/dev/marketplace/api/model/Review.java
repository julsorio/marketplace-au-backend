package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Entidad de MongoDB (colección {@code reviews}) que representa una reseña de un usuario hacia otro,
 * ligada a un listing/transacción concretos.
 * <p>
 * El índice compuesto único {@code listing_reviewer_reviewee_unique} garantiza que solo pueda existir
 * una reseña por listing entre un mismo reviewer y reviewee (no una única reseña global entre ambos
 * usuarios: el mismo par de usuarios puede reseñarse varias veces si completan transacciones sobre
 * distintos anuncios).
 */
@Data
@Document(collection = "reviews")
@CompoundIndex(name = "listing_reviewer_reviewee_unique", def = "{'listingId': 1, 'reviewerId': 1, 'revieweeId': 1}", unique = true)
public class Review {
    /** Identificador único de la reseña. */
    @Id
    private String id;

    /** Id del anuncio/transacción al que está ligada la reseña. */
    private String listingId;
    /** Id del usuario que escribe la reseña. */
    private String reviewerId;
    /** Id del usuario reseñado. */
    private String revieweeId;

    private int rating; // 1-5
    /** Comentario de la reseña. */
    private String comment;

    /** Fecha de creación de la reseña. */
    private Instant createdAt = Instant.now();
}
