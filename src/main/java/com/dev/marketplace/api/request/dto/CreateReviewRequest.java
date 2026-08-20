package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(@NotBlank String listingId,
        @NotBlank String revieweeId,
        @Min(1) @Max(5) int rating,
        @Size(max = 500) String comment) {
}
