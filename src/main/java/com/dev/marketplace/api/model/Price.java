package com.dev.marketplace.api.model;

/**
 * Precio de un listing, embebido dentro de la entidad {@link Listing}.
 *
 * @param amount      importe del precio
 * @param currency    código de la moneda (actualmente siempre "AUD")
 * @param negotiable  indica si el vendedor acepta negociar el precio
 */
public record Price(double amount, String currency, boolean negotiable) {}
