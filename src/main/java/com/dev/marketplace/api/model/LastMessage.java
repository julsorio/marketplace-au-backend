package com.dev.marketplace.api.model;

import java.time.Instant;

public record LastMessage(String text, Instant sentAt, String senderId) {}
