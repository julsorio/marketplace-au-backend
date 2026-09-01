package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando una operación de escritura viola una restricción de unicidad
 * (por ejemplo, un índice único de MongoDB). No tiene un {@code @ExceptionHandler} dedicado
 * en {@link GlobalExceptionHandler}, por lo que si no se captura en una capa inferior acaba
 * resolviéndose por el handler genérico y se traduce a un 500 Internal Server Error.
 */
public class DuplicateKeyException extends RuntimeException {

    /**
     * Crea la excepción sin mensaje adicional.
     */
    public DuplicateKeyException() {
        super();
    }
}
