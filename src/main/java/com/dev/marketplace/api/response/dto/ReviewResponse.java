package com.dev.marketplace.api.response.dto;

import java.time.Instant;

public record ReviewResponse(String id,
        String listingId,
        String reviewerId,
        String revieweeId,
        int rating,
        String comment,
        Instant createdAt) {
}
