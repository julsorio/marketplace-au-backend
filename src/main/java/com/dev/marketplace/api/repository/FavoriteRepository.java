package com.dev.marketplace.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Favorite;

public interface FavoriteRepository extends MongoRepository<Favorite, String> {
    List<Favorite> findByUserId(String userId);

    Optional<Favorite> findByUserIdAndListingId(String userId, String listingId);

    boolean existsByUserIdAndListingId(String userId, String listingId);

    void deleteByUserIdAndListingId(String userId, String listingId);
}
