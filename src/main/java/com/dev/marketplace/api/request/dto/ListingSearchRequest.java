package com.dev.marketplace.api.request.dto;

public record ListingSearchRequest(String category,
    String condition,
    Double minPrice,
    Double maxPrice,
    String state,
    Double latitude,
    Double longitude,
    Double radiusKm,
    String query, // búsqueda de texto libre en título/descripción
    int page,
    int size) {}
