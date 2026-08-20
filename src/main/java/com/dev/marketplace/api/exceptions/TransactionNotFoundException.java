package com.dev.marketplace.api.exceptions;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String id) {
        super("Transaccion no encontrada " + id);
    }
}
