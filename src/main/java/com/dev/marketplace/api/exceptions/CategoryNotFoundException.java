package com.dev.marketplace.api.exceptions;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String category) {
        super("Categoria no valida " + category);
    }
}
