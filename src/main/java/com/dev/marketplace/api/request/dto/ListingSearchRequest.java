package com.dev.marketplace.api.request.dto;

/**
 * Criterios de búsqueda y paginación para consultar listings activos. Todos los campos de
 * filtro son opcionales (pueden venir a {@code null}); ver
 * {@link com.dev.marketplace.api.repository.ListingSearchRepository#search(ListingSearchRequest)}
 * para el detalle de cómo se combinan.
 *
 * @param category  filtro opcional por categoría
 * @param condition filtro opcional por condición del producto
 * @param minPrice  precio mínimo opcional
 * @param maxPrice  precio máximo opcional
 * @param state     filtro opcional por estado/región australiana
 * @param latitude  latitud opcional del punto de referencia para la búsqueda geoespacial por radio
 * @param longitude longitud opcional del punto de referencia para la búsqueda geoespacial por radio
 * @param radiusKm  radio opcional, en kilómetros, de la búsqueda geoespacial
 * @param query     búsqueda de texto libre en título/descripción
 * @param page      número de página (0-indexado)
 * @param size      tamaño de página
 */
public record ListingSearchRequest(String category,
    String condition,
    Double minPrice,
    Double maxPrice,
    String state,
    Double latitude,
    Double longitude,
    Double radiusKm,
    String query,
    int page,
    int size) {}
