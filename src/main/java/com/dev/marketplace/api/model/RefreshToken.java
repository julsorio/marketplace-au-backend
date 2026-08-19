package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "refresh_tokens")
public class RefreshToken {
@Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash; // nunca guardar el token en texto plano

    private String userId;
    private Instant expiresAt;
    private boolean revoked = false;
    private Instant createdAt = Instant.now();
}
