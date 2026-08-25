package com.dev.marketplace.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "listings")
public class Listing {
    @Id
    private String id;

    private String sellerId;

    private String title;
    private String description;

    private Price price;

    @Indexed
    private String category;
    private String subcategory;

    private String condition; // new | like_new | good | fair

    private String deliveryMethod = "in_person"; // shipping | in_person | both

    private Map<String, Object> attributes;

    private List<String> images;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String suburb;

    @Indexed
    private String state;

    @Indexed
    private String status = "active"; // active | sold | reserved | expired | draft

    private int views = 0;
    private int favoritesCount = 0;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant expiresAt;
}
