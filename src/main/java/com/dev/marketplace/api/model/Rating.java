package com.dev.marketplace.api.model;

/**
 * Rating agregado de un usuario como vendedor/comprador, embebido en {@link User#getRating()}
 * y expuesto también en {@code UserPublicResponse}.
 *
 * @param average puntuación media de las reseñas recibidas por el usuario
 * @param count   número total de reseñas que componen la media
 */
public record Rating(double average, int count) {

}
