package com.dev.marketplace.api.request.dto;

/**
 * Resumen del usuario autenticado (UserSummary), devuelto en las respuestas de
 * autenticación y en el endpoint {@code /user/me}. A diferencia de UserPublicResponse,
 * sí incluye el email porque solo se expone al propio usuario.
 *
 * @param id          id del usuario
 * @param email       email del usuario
 * @param displayName nombre público del usuario
 * @param avatarUrl   URL del avatar del usuario
 */
public record UserSummary(String id, String email, String displayName, String avatarUrl) {}
