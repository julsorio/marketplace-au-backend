package com.dev.marketplace.api.response.dto;

import java.util.List;

/**
 * Datos de una categoría (o subcategoría) devueltos por la API.
 *
 * @param id             id (slug) de la categoría
 * @param name           nombre de la categoría
 * @param icon           icono asociado a la categoría
 * @param subcategories  subcategorías directas de esta categoría; vacía cuando la propia categoría ya es una subcategoría, ya que el árbol de categorías solo tiene dos niveles
 */
public record CategoryResponse(String id,
        String name,
        String icon,
        List<CategoryResponse> subcategories) {}
