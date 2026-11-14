package com.dev.marketplace.api.exceptions;

/**
 * La petición al webhook no trae una firma válida de Stripe.
 */
public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException() {
        super("Firma del webhook no válida");
    }
}
