package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos de acceso para el endpoint de login.
 *
 * @param email    email del usuario; debe tener formato de email y no estar en blanco
 * @param password contraseña en texto plano del usuario; no puede estar en blanco
 */
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
