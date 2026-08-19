package com.dev.marketplace.api.request.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * representa la peticion para el alta de usuario
 * RegisterRequest
 */
public record RegisterRequest (
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String displayName,
    String phone
) {}
