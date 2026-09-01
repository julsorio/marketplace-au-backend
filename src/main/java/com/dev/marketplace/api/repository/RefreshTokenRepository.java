package com.dev.marketplace.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.RefreshToken;

/**
 * Repositorio Spring Data MongoDB para la colección {@code refresh_tokens}, usado
 * para localizar un refresh token a partir de su hash durante el flujo de renovación.
 */
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    /**
     * Busca un refresh token por el hash del token en texto plano (nunca se busca ni
     * se guarda el token original).
     *
     * @param tokenHash hash (SHA-256 en Base64 URL-safe) del refresh token a buscar
     * @return el refresh token cuyo hash coincide, o vacío si no existe ninguno
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
