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

/**
 * Gestiona la creación y consulta de reseñas entre usuarios.
 * <p>
 * Cada reseña queda ligada a un listing/transacción concretos: un mismo par de usuarios puede
 * reseñarse varias veces si han completado transacciones sobre distintos anuncios, pero solo puede
 * existir una reseña por listing entre un mismo reviewer y reviewee (no una única reseña global entre
 * ambos usuarios). Intentar crear una reseña duplicada para el mismo listing se rechaza con un 409
 * mediante {@link DuplicateReviewException}.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Crea una reseña de un usuario hacia otro, asociada a un listing concreto, y recalcula la
     * valoración media del usuario reseñado.
     * <p>
     * Solo se permite reseñar cuando existe una transacción confirmada sobre ese listing entre ambos
     * usuarios (en cualquiera de los dos sentidos vendedor/comprador), y no se puede reseñar a uno
     * mismo. Como la reseña queda ligada al listing, solo se admite una reseña por listing entre el
     * mismo reviewer y reviewee: un segundo intento se rechaza como duplicado (409).
     *
     * @param reviewerId id del usuario autenticado que escribe la reseña
     * @param request datos de la reseña: listing, usuario reseñado, puntuación y comentario
     * @return la reseña creada
     * @throws ReviewNotAllowedException si el reviewer intenta reseñarse a sí mismo, o si no existe
     *         una transacción confirmada entre ambos usuarios sobre ese listing
     * @throws DuplicateReviewException si ya existe una reseña de este reviewer hacia este reviewee
     *         para el mismo listing
     */
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

    /**
     * Obtiene las reseñas recibidas por un usuario, ordenadas de más reciente a más antigua.
     *
     * @param userId id del usuario reseñado
     * @return lista de reseñas recibidas por el usuario
     */
    public List<ReviewResponse> getReviewsForUser(String userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Recalcula y persiste la valoración media y el número de reseñas de un usuario a partir de
     * todas las reseñas que ha recibido hasta el momento.
     *
     * @param userId id del usuario cuya valoración se recalcula
     */
    private void updateUserRating(String userId) {
        List<Review> reviews = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        int count = reviews.size();

        userRepository.findById(userId).ifPresent(user -> {
            user.setRating(new Rating(Math.round(average * 10.0) / 10.0, count));
            userRepository.save(user);
        });
    }

    /**
     * Convierte una entidad {@link Review} en su DTO de respuesta {@link ReviewResponse}.
     *
     * @param r reseña a convertir
     * @return el DTO de respuesta correspondiente
     */
    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(), r.getListingId(), r.getReviewerId(), r.getRevieweeId(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
