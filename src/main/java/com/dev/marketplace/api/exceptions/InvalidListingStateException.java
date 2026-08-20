package com.dev.marketplace.api.exceptions;

public class InvalidListingStateException extends RuntimeException {
    public InvalidListingStateException(String message) {
        super(message);
    }
}
