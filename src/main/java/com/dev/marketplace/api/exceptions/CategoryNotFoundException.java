package com.dev.marketplace.api.exceptions;

/**
 * Excepción lanzada cuando se referencia una categoría que no existe o no es válida
 * (por ejemplo, al crear o actualizar un listing con una categoría desconocida).
 * Se traduce a un 400 Bad Request mediante {@link GlobalExceptionHandler}.
 */
public class CategoryNotFoundException extends RuntimeException {

    /**
     * Crea la excepción indicando la categoría que no se ha podido resolver.
     *
     * @param category nombre o identificador de la categoría no válida
     */
    public CategoryNotFoundException(String category) {
        super("Categoria no valida " + category);
    }
}
