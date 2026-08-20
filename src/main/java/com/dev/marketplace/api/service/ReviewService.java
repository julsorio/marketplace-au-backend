package com.dev.marketplace.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.DuplicateReviewException;
import com.dev.marketplace.api.exceptions.ReviewNotAllowedException;
import com.dev.marketplace.api.model.Rating;
import com.dev.marketplace.api.model.Review;
import com.dev.marketplace.api.repository.ReviewRepository;
import com.dev.marketplace.api.repository.TransactionRepository;
import com.dev.marketplace.api.repository.UserRepository;
import com.dev.marketplace.api.request.dto.CreateReviewRequest;
import com.dev.marketplace.api.response.dto.ReviewResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ReviewResponse createReview(String reviewerId, CreateReviewRequest request) {
        if (reviewerId.equals(request.revieweeId())) {
            throw new ReviewNotAllowedException("No puedes reseñarte a ti mismo");
        }

        boolean hadConfirmedTransaction = transactionRepository.existsByListingIdAndStatusAndSellerIdAndBuyerId(
                request.listingId(), "confirmed", reviewerId, request.revieweeId())
                || transactionRepository.existsByListingIdAndStatusAndSellerIdAndBuyerId(
                        request.listingId(), "confirmed", request.revieweeId(), reviewerId);

        if (!hadConfirmedTransaction) {
            throw new ReviewNotAllowedException(
                    "Solo puedes reseñar a usuarios con los que completaste una transacción confirmada sobre este anuncio");
        }

        if (reviewRepository.existsByListingIdAndReviewerIdAndRevieweeId(
                request.listingId(), reviewerId, request.revieweeId())) {
            throw new DuplicateReviewException();
        }

        Review review = new Review();
        review.setListingId(request.listingId());
        review.setReviewerId(reviewerId);
        review.setRevieweeId(request.revieweeId());
        review.setRating(request.rating());
        review.setComment(request.comment());

        Review saved = reviewRepository.save(review);

        updateUserRating(request.revieweeId());

        return toResponse(saved);
    }

    public List<ReviewResponse> getReviewsForUser(String userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void updateUserRating(String userId) {
        List<Review> reviews = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        int count = reviews.size();

        userRepository.findById(userId).ifPresent(user -> {
            user.setRating(new Rating(Math.round(average * 10.0) / 10.0, count));
            userRepository.save(user);
        });
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(), r.getListingId(), r.getReviewerId(), r.getRevieweeId(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
