package com.dev.marketplace.api.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * representacion de la coleccion users
 * User
 */
@Data
@Document(collection = "users")
public class User {
@Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String displayName;
    private String phone;
    private String avatarUrl;

    private GeoJsonPoint location; // Spring Data MongoDB ya soporta GeoJsonPoint
    private String suburb;
    private String state;
    private String postcode;

    private Rating rating = new Rating(0.0, 0);
    private Verified verified = new Verified(false, false);

    private List<String> roles = List.of("ROLE_USER");

    private Instant createdAt = Instant.now();
    private String status = "active";
}
