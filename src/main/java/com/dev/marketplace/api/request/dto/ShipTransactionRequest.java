package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del envío que registra el vendedor.
 *
 * @param trackingNumber número de seguimiento del paquete en la empresa de transporte
 */
public record ShipTransactionRequest(@NotBlank @Size(max = 64) String trackingNumber) {}
