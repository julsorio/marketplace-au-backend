package com.dev.marketplace.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Entidad de MongoDB (colección {@code transactions}) que representa una transacción de compraventa
 * sobre un anuncio.
 * <p>
 * El ciclo de estados de la transacción es pending → confirmed/cancelled, en paralelo al ciclo de
 * estados del anuncio asociado: activo → reservado → vendido/cancelado.
 */
@Data
@Document(collection = "transactions")
public class Transaction {
    /** Identificador único de la transacción. */
    @Id
    private String id;

    /** Id del anuncio (Listing) sobre el que versa la transacción. Indexado para búsquedas por anuncio. */
    @Indexed
    private String listingId;

    /** Id del usuario vendedor. */
    private String sellerId;
    /** Id del usuario comprador. */
    private String buyerId;

    /** Importe de la transacción. */
    private double amount;
    /** Moneda de la transacción. */
    private String currency;

    private String paymentMethod; // "card" | "in_person"
    private String status; // "pending" | "confirmed" | "cancelled"

    /**
     * Referencia al PaymentIntent de Stripe. Solo se rellena cuando paymentMethod = "card"; la
     * confirmación automática de pagos con tarjeta vía Stripe todavía no está implementada.
     */
    private String paymentIntentId; // solo si paymentMethod = "card" (referencia a Stripe)

    /** Fecha de creación de la transacción. */
    private Instant createdAt = Instant.now();
    /** Fecha en la que se confirmó la transacción; {@code null} mientras está pendiente o si se canceló. */
    private Instant confirmedAt;
}
