package com.dev.marketplace.api.response.dto;

import java.time.Instant;

public record TransactionResponse(String id,
        String listingId,
        String sellerId,
        String buyerId,
        double amount,
        String currency,
        String paymentMethod,
        String status,
        Instant createdAt,
        Instant confirmedAt) {
}
