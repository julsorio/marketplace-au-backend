package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Documento MongoDB que representa que un usuario ha marcado un listing como favorito.
 * El índice compuesto único sobre {@code userId} y {@code listingId} garantiza a nivel de
 * base de datos que un usuario no pueda tener el mismo listing duplicado en sus favoritos.
 */
@Data
@Document(collection = "favorites")
@CompoundIndex(name = "user_listing_unique", def = "{'userId': 1, 'listingId': 1}", unique = true)
public class Favorite {
    @Id
    private String id;

    /** Identificador del usuario que marcó el listing como favorito. */
    private String userId;

    /** Identificador del listing marcado como favorito. */
    private String listingId;

    private Instant createdAt = Instant.now();
}
