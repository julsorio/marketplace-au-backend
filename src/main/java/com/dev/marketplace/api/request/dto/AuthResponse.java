package com.dev.marketplace.api.request.dto;

public record AuthResponse( 
    String accessToken,
    String refreshToken,
    UserSummary user) {}
