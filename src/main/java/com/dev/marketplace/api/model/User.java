package com.dev.marketplace.api.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Entidad que representa un usuario del marketplace, persistida en la colección {@code users}.
 * Incluye los datos de contacto y ubicación del usuario, su rating agregado como vendedor/comprador,
 * el estado de verificación de email/teléfono y los roles usados para autorización.
 */
@Data
@Document(collection = "users")
public class User {
@Id
    private String id;

    /** Email del usuario, usado como identificador de login; único en la colección. */
    @Indexed(unique = true)
    private String email;

    /** Hash de la contraseña (nunca se guarda en texto plano). */
    private String passwordHash;
    private String displayName;
    private String phone;
    private String avatarUrl;

    private GeoJsonPoint location; // Spring Data MongoDB ya soporta GeoJsonPoint
    private String suburb;
    private String state;
    private String postcode;

    /** Rating agregado del usuario (promedio y número de reseñas), embebido en el documento. */
    private Rating rating = new Rating(0.0, 0);

    /** Estado de verificación de email y teléfono del usuario, embebido en el documento. */
    private Verified verified = new Verified(false, false);

    /** Roles de autorización del usuario; por defecto todo usuario nuevo recibe ROLE_USER. */
    private List<String> roles = List.of("ROLE_USER");

    private Instant createdAt = Instant.now();

    /** Estado de la cuenta (p. ej. "active"), usado para habilitar o deshabilitar el acceso. */
    private String status = "active";
}
