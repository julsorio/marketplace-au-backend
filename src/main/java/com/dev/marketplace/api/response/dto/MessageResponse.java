package com.dev.marketplace.api.response.dto;

public record MessageResponse(String id,
        String conversationId,
        String senderId,
        String text,
        java.time.Instant sentAt,
        boolean read) {
}
