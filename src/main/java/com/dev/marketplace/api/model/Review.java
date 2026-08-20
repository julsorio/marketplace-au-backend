package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "reviews")
@CompoundIndex(name = "listing_reviewer_reviewee_unique", def = "{'listingId': 1, 'reviewerId': 1, 'revieweeId': 1}", unique = true)
public class Review {
    @Id
    private String id;

    private String listingId;
    private String reviewerId;
    private String revieweeId;

    private int rating; // 1-5
    private String comment;

    private Instant createdAt = Instant.now();
}
