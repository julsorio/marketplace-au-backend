package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "favorites")
@CompoundIndex(name = "user_listing_unique", def = "{'userId': 1, 'listingId': 1}", unique = true)
public class Favorite {
    @Id
    private String id;

    private String userId;
    private String listingId;

    private Instant createdAt = Instant.now();
}
