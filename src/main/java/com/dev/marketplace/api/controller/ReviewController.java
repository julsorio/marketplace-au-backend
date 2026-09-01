package com.dev.marketplace.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.request.dto.CreateReviewRequest;
import com.dev.marketplace.api.response.dto.ReviewResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Expone los endpoints REST para crear reseñas y consultar las reseñas recibidas por un usuario.
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    /**
     * Crea una reseña del usuario autenticado hacia otro usuario, asociada a un listing.
     *
     * @param principal usuario autenticado que escribe la reseña
     * @param request datos de la reseña (listing, usuario reseñado, puntuación y comentario)
     * @return 201 Created con la reseña creada
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(principal.getUserId(), request));
    }

    /**
     * Obtiene las reseñas recibidas por un usuario.
     *
     * @param userId id del usuario reseñado
     * @return 200 OK con la lista de reseñas recibidas
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForUser(@PathVariable String userId) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId));
    }
}
