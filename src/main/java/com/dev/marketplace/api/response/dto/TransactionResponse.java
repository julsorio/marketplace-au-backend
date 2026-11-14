package com.dev.marketplace.api.response.dto;

import java.time.Instant;

/**
 * Datos de una transacción devueltos por la API. Nunca incluye el {@code clientSecret} de Stripe:
 * esta respuesta también la recibe el vendedor (ver {@link PaymentIntentResponse}).
 *
 * @param id identificador de la transacción
 * @param listingId id del anuncio sobre el que versa la transacción
 * @param sellerId id del usuario vendedor
 * @param buyerId id del usuario comprador
 * @param amount importe de la transacción
 * @param currency moneda de la transacción
 * @param paymentMethod método de pago: "card" o "in_person"
 * @param deliveryType tipo de entrega: "shipping" o "in_person"
 * @param status estado de la transacción (ver {@code TransactionStatus})
 * @param platformFee comisión de la plataforma descontada al vendedor (0 si es en efectivo)
 * @param trackingNumber número de seguimiento del envío, o {@code null}
 * @param createdAt fecha de creación de la transacción
 * @param paidAt fecha del pago con tarjeta, o {@code null}
 * @param acceptedAt fecha en la que el vendedor aceptó la compra, o {@code null}
 * @param shippedAt fecha del envío, o {@code null}
 * @param confirmedAt fecha de confirmación de la transacción, o {@code null} si aún no se ha
 *        confirmado
 * @param paymentExpiresAt límite para completar el pago, o {@code null}
 * @param acceptDeadline límite para que el vendedor acepte, o {@code null}
 * @param shipDeadline límite para que el vendedor envíe, o {@code null}
 * @param autoConfirmAt fecha en la que la entrega se confirmará sola si el comprador no lo hace, o
 *        {@code null}
 */
public record TransactionResponse(String id,
        String listingId,
        String sellerId,
        String buyerId,
        double amount,
        String currency,
        String paymentMethod,
        String deliveryType,
        String status,
        double platformFee,
        String trackingNumber,
        Instant createdAt,
        Instant paidAt,
        Instant acceptedAt,
        Instant shippedAt,
        Instant confirmedAt,
        Instant paymentExpiresAt,
        Instant acceptDeadline,
        Instant shipDeadline,
        Instant autoConfirmAt) {
}
