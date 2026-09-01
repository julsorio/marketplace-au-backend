package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Representa la petición para el alta de un nuevo usuario (RegisterRequest).
 *
 * @param email       email del usuario; debe tener formato de email y no estar en blanco
 * @param password    contraseña en texto plano; no puede estar en blanco y debe tener al menos 8 caracteres
 * @param displayName nombre público del usuario; no puede estar en blanco
 * @param phone       teléfono de contacto del usuario; opcional
 */
public record RegisterRequest (
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String displayName,
    String phone
) {}
