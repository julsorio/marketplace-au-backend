package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Entidad que representa un refresh token emitido a un usuario, persistida en la colección
 * {@code refresh_tokens}. Por seguridad nunca se guarda el token en texto plano: solo se
 * persiste su hash, junto con su expiración y si ha sido revocado (p. ej. tras rotarlo).
 */
@Data
@Document(collection = "refresh_tokens")
public class RefreshToken {
@Id
    private String id;

    /** Hash del refresh token; nunca se guarda el token en texto plano. */
    @Indexed(unique = true)
    private String tokenHash;

    /** Id del usuario ({@link User#getId()}) propietario de este refresh token. */
    private String userId;
    private Instant expiresAt;

    /** {@code true} una vez que el token ha sido canjeado (rotado) o invalidado manualmente. */
    private boolean revoked = false;
    private Instant createdAt = Instant.now();
}
