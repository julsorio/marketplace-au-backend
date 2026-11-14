package com.dev.marketplace.api.model;

/**
 * Estados posibles de una {@link Transaction}.
 * <p>
 * Pago en persona (efectivo): {@code pending} → {@code confirmed} / {@code cancelled}.
 * <p>
 * Pago con tarjeta (Stripe): {@code awaiting_payment} → {@code awaiting_acceptance} →
 * {@code accepted} → ({@code shipped}, solo con envío) → {@code confirmed}, o
 * {@code cancelled} en cualquier punto antes de la entrega según la política de cancelación.
 */
public final class TransactionStatus {
    /** Pago en persona reservado, a la espera de que el vendedor confirme la venta. */
    public static final String PENDING = "pending";
    /** Tarjeta: reserva creada, el comprador todavía no ha completado el pago. */
    public static final String AWAITING_PAYMENT = "awaiting_payment";
    /** Tarjeta: pago recibido, el vendedor tiene 24 h para aceptar o rechazar la compra. */
    public static final String AWAITING_ACCEPTANCE = "awaiting_acceptance";
    /** Tarjeta: el vendedor aceptó; pendiente de envío o de entrega en persona. */
    public static final String ACCEPTED = "accepted";
    /** Tarjeta: el vendedor registró el número de seguimiento del envío. */
    public static final String SHIPPED = "shipped";
    /** Venta completada (anuncio vendido). Con tarjeta, el dinero ya se transfirió al vendedor. */
    public static final String CONFIRMED = "confirmed";
    /** Transacción cancelada; con tarjeta, el pago (si lo hubo) se reembolsó al comprador. */
    public static final String CANCELLED = "cancelled";

    private TransactionStatus() {
    }
}
