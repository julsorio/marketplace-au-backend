package com.dev.marketplace.api.exceptions;

public class SelfMessagingNotAllowedException extends RuntimeException {
    public SelfMessagingNotAllowedException() {
        super("No puedes enviarte un mensaje a ti mismo");
    }
}
