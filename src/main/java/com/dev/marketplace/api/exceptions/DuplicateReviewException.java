package com.dev.marketplace.api.exceptions;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException() {
        super("Ya has dejado una reseña para esta transacción");
    }
}
