package com.dev.marketplace.api.response.dto;

import java.time.Instant;

/**
 * Datos de un favorito devueltos al cliente, incluyendo el detalle completo del listing
 * favorito (no solo su identificador), para que el cliente pueda mostrarlo directamente
 * sin necesidad de una consulta adicional.
 *
 * @param listing     detalle completo del listing marcado como favorito
 * @param favoritedAt fecha y hora en que el listing fue marcado como favorito
 */
public record FavoriteResponse(ListingResponse listing,
        Instant favoritedAt) {}
