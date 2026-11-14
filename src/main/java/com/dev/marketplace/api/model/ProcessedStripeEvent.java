package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evento de webhook de Stripe ya procesado (colección {@code stripe_events}). Stripe puede reenviar
 * el mismo evento varias veces, así que guardamos su id para no aplicarlo dos veces.
 *
 * @param id          id del evento en Stripe ({@code evt_...})
 * @param type        tipo del evento, solo informativo
 * @param processedAt fecha en la que se procesó
 */
@Document(collection = "stripe_events")
public record ProcessedStripeEvent(@Id String id, String type, Instant processedAt) {}
