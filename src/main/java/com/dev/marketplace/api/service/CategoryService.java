package com.dev.marketplace.api.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.model.Category;
import com.dev.marketplace.api.repository.CategoryRepository;
import com.dev.marketplace.api.response.dto.CategoryResponse;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de negocio para el catálogo de categorías del marketplace, organizado en un árbol
 * de dos niveles (categorías raíz y sus subcategorías directas).
 */
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    /**
     * Construye el árbol completo de categorías: obtiene las categorías raíz (sin
     * {@code parentId}) ordenadas por {@code displayOrder} y, para cada una, adjunta sus
     * subcategorías directas (también ordenadas por {@code displayOrder}).
     *
     * @return la lista de categorías raíz, cada una con su lista de subcategorías anidada
     */
    public List<CategoryResponse> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIdIsNull();

        return roots.stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(this::toResponseWithChildren)
                .toList();
    }

    /**
     * Convierte una categoría raíz en su {@link CategoryResponse}, resolviendo y ordenando
     * sus subcategorías directas. Las subcategorías se devuelven sin su propia lista de hijos
     * (siempre vacía), ya que el árbol de categorías solo tiene dos niveles.
     *
     * @param category categoría raíz a convertir
     * @return el DTO de la categoría con sus subcategorías anidadas
     */
    private CategoryResponse toResponseWithChildren(Category category) {
        List<CategoryResponse> children = categoryRepository.findByParentId(category.getId()).stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(sub -> new CategoryResponse(sub.getId(), sub.getName(), sub.getIcon(), List.of()))
                .toList();

        return new CategoryResponse(category.getId(), category.getName(), category.getIcon(), children);
    }

    /**
     * Comprueba si existe una categoría con el id indicado.
     * Se usa para validar la categoría al crear un listing.
     *
     * @param categoryId id (slug) de la categoría a comprobar
     * @return {@code true} si existe una categoría con ese id, {@code false} en caso contrario
     */
    public boolean categoryExists(String categoryId) {
        return categoryRepository.existsById(categoryId);
    }
}
