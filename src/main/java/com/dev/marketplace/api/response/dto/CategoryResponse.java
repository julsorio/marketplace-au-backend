package com.dev.marketplace.api.response.dto;

import java.util.List;

public record CategoryResponse(String id,
        String name,
        String icon,
        List<CategoryResponse> subcategories) {}
