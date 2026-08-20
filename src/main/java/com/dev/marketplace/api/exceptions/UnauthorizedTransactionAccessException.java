package com.dev.marketplace.api.exceptions;

public class UnauthorizedTransactionAccessException extends RuntimeException {
    public UnauthorizedTransactionAccessException() {
        super("No tienes permiso para esta operación sobre la transacción");
    }

}
