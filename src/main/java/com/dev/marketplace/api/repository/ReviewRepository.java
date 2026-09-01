package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Review;

/**
 * Repositorio Spring Data MongoDB para el acceso a {@link Review}.
 */
public interface ReviewRepository extends MongoRepository<Review, String> {
    /**
     * Busca las reseñas recibidas por el usuario indicado, ordenadas de más reciente a más antigua.
     *
     * @param revieweeId id del usuario reseñado
     * @return lista de reseñas recibidas por el usuario
     */
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(String revieweeId);

    /**
     * Comprueba si ya existe una reseña para el listing indicado, escrita por el reviewer hacia el
     * reviewee dados. Se usa para evitar reseñas duplicadas sobre un mismo listing.
     *
     * @param listingId id del anuncio/transacción
     * @param reviewerId id del usuario que escribe la reseña
     * @param revieweeId id del usuario reseñado
     * @return {@code true} si ya existe una reseña que cumpla todas las condiciones
     */
    boolean existsByListingIdAndReviewerIdAndRevieweeId(String listingId, String reviewerId, String revieweeId);

}
