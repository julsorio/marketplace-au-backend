package com.dev.marketplace.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Favorite;

/**
 * Repositorio Spring Data MongoDB para el acceso a la colección de favoritos.
 */
public interface FavoriteRepository extends MongoRepository<Favorite, String> {

    /**
     * Busca todos los favoritos de un usuario.
     *
     * @param userId identificador del usuario
     * @return los favoritos del usuario
     */
    List<Favorite> findByUserId(String userId);

    /**
     * Busca el favorito de un usuario para un listing concreto, si existe.
     *
     * @param userId    identificador del usuario
     * @param listingId identificador del listing
     * @return el favorito encontrado, o vacío si el usuario no tiene ese listing marcado como favorito
     */
    Optional<Favorite> findByUserIdAndListingId(String userId, String listingId);

    /**
     * Comprueba si un usuario ya tiene marcado como favorito un listing concreto.
     *
     * @param userId    identificador del usuario
     * @param listingId identificador del listing
     * @return {@code true} si el listing ya es favorito del usuario, {@code false} en caso contrario
     */
    boolean existsByUserIdAndListingId(String userId, String listingId);

    /**
     * Elimina el favorito de un usuario para un listing concreto, si existe.
     *
     * @param userId    identificador del usuario
     * @param listingId identificador del listing
     */
    void deleteByUserIdAndListingId(String userId, String listingId);
}
