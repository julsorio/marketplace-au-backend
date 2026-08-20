package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ReserveTransactionRequest(@NotBlank String listingId,
        @NotBlank String buyerId,
        @NotNull @DecimalMin("0.0") Double amount,
        @NotBlank @Pattern(regexp = "card|in_person", message = "Método de pago no válido") String paymentMethod) {}
