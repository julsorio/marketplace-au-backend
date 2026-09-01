package com.dev.marketplace.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.response.dto.CategoryResponse;
import com.dev.marketplace.api.service.CategoryService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint REST de solo lectura para el árbol de categorías del marketplace.
 * Delega toda la lógica en {@link CategoryService}.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * Devuelve el árbol completo de categorías (categorías raíz con sus subcategorías anidadas).
     *
     * @return la lista de categorías raíz, cada una con su lista de subcategorías
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }
}
