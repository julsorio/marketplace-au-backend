package com.dev.marketplace.api.exceptions;

public class ReviewNotAllowedException extends RuntimeException {
    public ReviewNotAllowedException(String message) {
        super(message);
    }

}
