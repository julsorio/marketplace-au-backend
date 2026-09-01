package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de entrada para cambiar el estado del ciclo de vida de un listing.
 *
 * @param status nuevo estado del listing; debe ser uno de {@code active}, {@code sold},
 *               {@code reserved}, {@code expired} o {@code draft}
 */
public record UpdateListingStatusRequest(@NotBlank
    @Pattern(regexp = "active|sold|reserved|expired|draft", message = "Estado no válido")
    String status) {}
