package com.dev.marketplace.api.response.dto;

import com.dev.marketplace.api.model.Rating;

/**
 * Perfil público mínimo de un usuario, para mostrar quién es el otro participante de una
 * conversación o el vendedor de un anuncio. A diferencia de UserSummary (usado en /user/me),
 * deliberadamente NO incluye email ni ningún otro dato sensible. Se incluye el rating agregado
 * (promedio y número de reseñas) para que el frontend pueda mostrarlo junto al vendedor/comprador.
 *
 * @param id          id del usuario
 * @param displayName nombre público del usuario
 * @param avatarUrl   URL del avatar del usuario
 * @param rating      rating agregado del usuario (promedio y número de reseñas)
 */
public record UserPublicResponse(String id, String displayName, String avatarUrl, Rating rating) {}
