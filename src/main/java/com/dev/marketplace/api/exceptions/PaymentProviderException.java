package com.dev.marketplace.api.exceptions;

/**
 * Error al comunicarse con la pasarela de pago (Stripe), o pasarela sin configurar.
 */
public class PaymentProviderException extends RuntimeException {
    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
