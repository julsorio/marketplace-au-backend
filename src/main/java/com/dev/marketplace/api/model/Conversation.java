package com.dev.marketplace.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;

    @Indexed
    private String listingId;

    @Indexed
    private List<String> participants; // [buyerId, sellerId]

    private LastMessage lastMessage;

    private Map<String, Integer> unreadCount; // { userId: count }

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
