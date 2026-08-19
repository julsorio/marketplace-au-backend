package com.dev.marketplace.api.response.dto;

import java.time.Instant;

public record ConversationResponse(String id,
        String listingId,
        java.util.List<String> participants,
        String lastMessageText,
        Instant lastMessageSentAt,
        int unreadCount) {
}
