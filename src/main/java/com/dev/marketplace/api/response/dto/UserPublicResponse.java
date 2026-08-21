package com.dev.marketplace.api.response.dto;

/**
 * Perfil público mínimo de un usuario, para mostrar quién es el otro participante de una
 * conversación o el vendedor de un anuncio. A diferencia de UserSummary (usado en /user/me),
 * deliberadamente NO incluye email ni ningún otro dato sensible.
 */
public record UserPublicResponse(String id, String displayName, String avatarUrl) {}
