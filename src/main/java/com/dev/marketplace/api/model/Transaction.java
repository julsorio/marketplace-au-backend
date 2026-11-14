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
 * Los estados posibles y su ciclo están en {@link TransactionStatus}. En paralelo, el anuncio asociado
 * pasa de activo → reservado → vendido, o vuelve a activo si la transacción se cancela.
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

    /** Importe de la transacción: siempre el precio del anuncio en el momento de la reserva. */
    private double amount;
    /** Moneda de la transacción. */
    private String currency;

    private String paymentMethod; // "card" | "in_person"
    private String deliveryType; // "shipping" | "in_person"
    @Indexed
    private String status; // ver TransactionStatus

    /** Comisión de la plataforma descontada al vendedor (solo con tarjeta). */
    private double platformFee;

    /** Referencias de Stripe (solo con tarjeta). */
    private String paymentIntentId;
    private String chargeId;
    private String transferId;
    private String refundId;

    /** Número de seguimiento del envío, introducido por el vendedor. */
    private String trackingNumber;

    /** Fecha de creación de la transacción. */
    private Instant createdAt = Instant.now();
    private Instant paidAt;
    private Instant acceptedAt;
    private Instant shippedAt;
    /** Fecha en la que se confirmó la transacción; {@code null} mientras está pendiente o si se canceló. */
    private Instant confirmedAt;
    private Instant cancelledAt;

    /**
     * Plazos que vigila {@code TransactionScheduler}: pago sin completar, aceptación del vendedor,
     * envío y confirmación automática de la entrega.
     */
    private Instant paymentExpiresAt;
    private Instant acceptDeadline;
    private Instant shipDeadline;
    private Instant autoConfirmAt;
}
