package com.dev.marketplace.api.response.dto;

/**
 * Estado del alta del usuario como vendedor en Stripe Connect.
 *
 * @param hasAccount     {@code true} si ya tiene una cuenta conectada creada
 * @param payoutsEnabled {@code true} si el alta está completa y puede cobrar ventas con tarjeta
 */
public record ConnectStatusResponse(boolean hasAccount, boolean payoutsEnabled) {}
