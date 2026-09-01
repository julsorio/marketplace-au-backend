package com.dev.marketplace.api.model;

/**
 * Estado de verificación de un usuario, embebido en {@link User#getVerified()}.
 *
 * @param email indica si el email del usuario ha sido verificado
 * @param phone indica si el teléfono del usuario ha sido verificado
 */
public record Verified(boolean email, boolean phone) {

}
