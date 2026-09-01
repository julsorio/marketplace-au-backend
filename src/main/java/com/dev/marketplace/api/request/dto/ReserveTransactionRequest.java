package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos necesarios para reservar un anuncio y crear una transacción pendiente.
 *
 * @param listingId id del anuncio que se desea reservar
 * @param buyerId id del comprador; solo se tiene en cuenta cuando quien hace la petición es el
 *        vendedor (que elige a qué comprador reservárselo, típicamente tras negociar por chat), y se
 *        ignora si la petición la hace el propio comprador directamente
 * @param amount importe de la transacción
 * @param paymentMethod método de pago: "card" o "in_person"
 */
public record ReserveTransactionRequest(@NotBlank String listingId,
        @NotBlank String buyerId,
        @NotNull @DecimalMin("0.0") Double amount,
        @NotBlank @Pattern(regexp = "card|in_person", message = "Método de pago no válido") String paymentMethod) {}
