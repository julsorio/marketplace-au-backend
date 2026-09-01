package com.dev.marketplace.api.response.dto;

/**
 * Datos de un listing devueltos por la API.
 *
 * @param id             id del listing
 * @param sellerId       id del vendedor propietario del listing
 * @param title          título del listing
 * @param description    descripción del listing
 * @param price          importe del precio
 * @param currency       código de la moneda del precio (siempre "AUD")
 * @param negotiable     indica si el vendedor acepta negociar el precio
 * @param category       id (slug) de la categoría raíz del listing
 * @param subcategory    id (slug) de la subcategoría del listing
 * @param condition      condición del producto
 * @param deliveryMethod método de entrega del listing
 * @param images         URLs de las imágenes del listing
 * @param suburb         suburbio en el que se encuentra el listing
 * @param state          estado o región australiana en la que se encuentra el listing
 * @param latitude       latitud de la ubicación del listing; exacta si el que consulta es el dueño, difuminada en caso contrario (ver {@link com.dev.marketplace.api.service.ListingService})
 * @param longitude      longitud de la ubicación del listing; exacta si el que consulta es el dueño, difuminada en caso contrario
 * @param status         estado del ciclo de vida del listing
 * @param views          número de veces que se ha consultado el listing
 * @param favoritesCount número de veces que el listing ha sido marcado como favorito
 * @param createdAt      instante en el que se creó el listing
 */
public record ListingResponse(String id,
    String sellerId,
    String title,
    String description,
    double price,
    String currency,
    boolean negotiable,
    String category,
    String subcategory,
    String condition,
    String deliveryMethod,
    java.util.List<String> images,
    String suburb,
    String state,
    Double latitude,
    Double longitude,
    String status,
    int views,
    int favoritesCount,
    java.time.Instant createdAt) {}
