package com.dev.marketplace.api.response.dto;

import java.time.Instant;

/**
 * Datos de una transacción devueltos por la API.
 *
 * @param id identificador de la transacción
 * @param listingId id del anuncio sobre el que versa la transacción
 * @param sellerId id del usuario vendedor
 * @param buyerId id del usuario comprador
 * @param amount importe de la transacción
 * @param currency moneda de la transacción
 * @param paymentMethod método de pago: "card" o "in_person"
 * @param status estado de la transacción: "pending", "confirmed" o "cancelled"
 * @param createdAt fecha de creación de la transacción
 * @param confirmedAt fecha de confirmación de la transacción, o {@code null} si aún no se ha
 *        confirmado
 */
public record TransactionResponse(String id,
        String listingId,
        String sellerId,
        String buyerId,
        double amount,
        String currency,
        String paymentMethod,
        String status,
        Instant createdAt,
        Instant confirmedAt) {
}
