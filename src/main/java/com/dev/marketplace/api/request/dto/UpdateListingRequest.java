package com.dev.marketplace.api.request.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de entrada para actualizar un listing existente. No incluye ubicación ni estado:
 * la ubicación no es editable tras la creación, y el estado se gestiona por separado
 * mediante {@link UpdateListingStatusRequest}.
 *
 * @param title          nuevo título del listing
 * @param description    nueva descripción del listing
 * @param price          nuevo precio del listing, debe ser mayor o igual a 0
 * @param negotiable     indica si el vendedor acepta negociar el precio
 * @param category       id (slug) de la categoría raíz del listing
 * @param subcategory    id (slug) de la subcategoría, opcional
 * @param condition      condición del producto (ej. new, like_new, good, fair)
 * @param deliveryMethod método de entrega; debe ser uno de {@code shipping}, {@code in_person} o {@code both}
 * @param attributes     atributos adicionales específicos de la categoría, en formato libre clave-valor
 * @param images         URLs de las imágenes del listing
 */
public record UpdateListingRequest(@NotBlank String title,
    @NotBlank String description,
    @NotNull @DecimalMin("0.0") Double price,
    boolean negotiable,
    @NotBlank String category,
    String subcategory,
    @NotBlank String condition,
    @NotBlank @Pattern(regexp = "shipping|in_person|both", message = "Método de entrega no válido") String deliveryMethod,
    Map<String, Object> attributes,
    List<String> images) {}
