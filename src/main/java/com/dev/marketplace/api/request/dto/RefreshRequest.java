package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la petición para renovar la sesión canjeando un refresh token.
 *
 * @param refreshToken refresh token en texto plano previamente emitido al cliente; no puede estar en blanco
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
