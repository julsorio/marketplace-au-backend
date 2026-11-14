package com.dev.marketplace.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.ProcessedStripeEvent;

/**
 * Repositorio Spring Data MongoDB para los eventos de webhook de Stripe ya procesados.
 */
public interface ProcessedStripeEventRepository extends MongoRepository<ProcessedStripeEvent, String> {
}
