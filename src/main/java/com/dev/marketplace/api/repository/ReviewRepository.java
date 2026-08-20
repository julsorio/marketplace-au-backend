package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Review;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(String revieweeId);

    boolean existsByListingIdAndReviewerIdAndRevieweeId(String listingId, String reviewerId, String revieweeId);

}
