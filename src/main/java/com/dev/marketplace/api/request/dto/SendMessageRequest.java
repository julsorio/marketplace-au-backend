package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String listingId,
        @NotBlank String recipientId,
        @NotBlank String text) {
}
