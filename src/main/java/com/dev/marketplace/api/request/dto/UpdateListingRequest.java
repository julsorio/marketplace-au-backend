package com.dev.marketplace.api.request.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateListingRequest(@NotBlank String title,
    @NotBlank String description,
    @NotNull @DecimalMin("0.0") Double price,
    boolean negotiable,
    @NotBlank String category,
    String subcategory,
    @NotBlank String condition,
    Map<String, Object> attributes,
    List<String> images) {}
