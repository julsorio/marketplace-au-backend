package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;

    @Indexed
    private String listingId;

    private String sellerId;
    private String buyerId;

    private double amount;
    private String currency;

    private String paymentMethod; // "card" | "in_person"
    private String status; // "pending" | "confirmed" | "cancelled"

    private String paymentIntentId; // solo si paymentMethod = "card" (referencia a Stripe)

    private Instant createdAt = Instant.now();
    private Instant confirmedAt;
}
