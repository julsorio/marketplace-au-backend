package com.dev.marketplace.api.exceptions;

/**
 * El vendedor no ha completado el alta en Stripe Connect, así que no puede cobrar con tarjeta.
 */
public class SellerNotOnboardedException extends RuntimeException {
    public SellerNotOnboardedException() {
        super("El vendedor todavía no acepta pagos con tarjeta");
    }
}
