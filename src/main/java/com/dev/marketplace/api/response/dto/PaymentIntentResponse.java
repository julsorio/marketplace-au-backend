package com.dev.marketplace.api.response.dto;

/**
 * Secreto del PaymentIntent de Stripe, que el frontend necesita para montar el Payment Element.
 * Solo se devuelve al comprador de la transacción.
 *
 * @param clientSecret secreto de cliente del PaymentIntent
 */
public record PaymentIntentResponse(String clientSecret) {}
