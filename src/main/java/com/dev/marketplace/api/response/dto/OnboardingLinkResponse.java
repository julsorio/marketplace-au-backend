package com.dev.marketplace.api.response.dto;

/**
 * Enlace de un solo uso a la página de alta de Stripe Connect.
 *
 * @param url dirección a la que redirigir al vendedor
 */
public record OnboardingLinkResponse(String url) {}
