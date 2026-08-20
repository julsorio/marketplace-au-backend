package com.dev.marketplace.api.response.dto;

import java.time.Instant;

public record FavoriteResponse(ListingResponse listing,
        Instant favoritedAt) {}
