package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos necesarios para reservar un anuncio y crear una transacción.
 * <p>
 * No lleva importe: se toma siempre del precio del anuncio. Si el precio se negocia, el vendedor
 * lo ajusta en el anuncio antes de la reserva.
 *
 * @param listingId id del anuncio que se desea reservar
 * @param buyerId id del comprador; solo se tiene en cuenta cuando quien hace la petición es el
 *        vendedor (que elige a qué comprador reservárselo, típicamente tras negociar por chat), y se
 *        ignora si la petición la hace el propio comprador directamente
 * @param paymentMethod método de pago: "card" o "in_person"
 * @param deliveryType tipo de entrega: "shipping" o "in_person"; debe ser compatible con el método de
 *        entrega del anuncio
 */
public record ReserveTransactionRequest(@NotBlank String listingId,
        String buyerId,
        @NotBlank @Pattern(regexp = "card|in_person", message = "Método de pago no válido") String paymentMethod,
        @NotBlank @Pattern(regexp = "shipping|in_person", message = "Tipo de entrega no válido") String deliveryType) {}
