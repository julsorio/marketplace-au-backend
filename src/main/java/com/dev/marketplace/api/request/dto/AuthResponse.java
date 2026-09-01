package com.dev.marketplace.api.request.dto;

/**
 * Respuesta común a los endpoints de autenticación (registro, login y refresh),
 * con los tokens de sesión emitidos y el resumen del usuario autenticado.
 *
 * @param accessToken  access token JWT, a enviar en la cabecera Authorization de las siguientes peticiones
 * @param refreshToken refresh token en texto plano, a usar únicamente contra el endpoint de refresh
 * @param user         resumen del usuario autenticado
 */
public record AuthResponse( 
    String accessToken,
    String refreshToken,
    UserSummary user) {}
