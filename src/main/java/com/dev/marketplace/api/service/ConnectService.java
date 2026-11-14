package com.dev.marketplace.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.UserNotFoundException;
import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.UserRepository;
import com.dev.marketplace.api.response.dto.ConnectStatusResponse;
import com.dev.marketplace.api.response.dto.OnboardingLinkResponse;

import lombok.RequiredArgsConstructor;

/**
 * Alta de vendedores en Stripe Connect (cuentas Express). El vendedor no se registra en stripe.com:
 * la plataforma le crea la cuenta y él completa un formulario corto alojado por Stripe (identidad y
 * cuenta bancaria), al que se llega con el enlace de {@link #createOnboardingLink}.
 */
@Service
@RequiredArgsConstructor
public class ConnectService {
    private final UserRepository userRepository;
    private final StripeGateway stripeGateway;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Crea la cuenta conectada si el usuario aún no la tiene y devuelve el enlace de alta. Stripe
     * redirige de vuelta a {@code /payments/setup} del frontend al terminar ({@code return}) o si el
     * enlace caduca ({@code refresh}, y el frontend pide uno nuevo).
     */
    public OnboardingLinkResponse createOnboardingLink(String userId) {
        User user = findUser(userId);

        if (user.getStripeAccountId() == null) {
            user.setStripeAccountId(stripeGateway.createConnectedAccount(
                    user.getEmail(), user.getId(), publicProfileUrl(user.getId())));
            userRepository.save(user);
        }

        String setupUrl = frontendUrl + "/payments/setup";
        return new OnboardingLinkResponse(stripeGateway.createOnboardingLink(
                user.getStripeAccountId(), setupUrl + "?onboarding=refresh", setupUrl + "?onboarding=return"));
    }

    /**
     * Consulta a Stripe el estado del alta y lo guarda, sin esperar al webhook {@code account.updated}
     * (el usuario acaba de volver de la página de Stripe y quiere ver el resultado ya).
     */
    public ConnectStatusResponse getStatus(String userId) {
        User user = findUser(userId);
        if (user.getStripeAccountId() == null) {
            return new ConnectStatusResponse(false, false);
        }

        boolean enabled = stripeGateway.payoutsEnabled(user.getStripeAccountId());
        if (enabled != user.isStripePayoutsEnabled()) {
            user.setStripePayoutsEnabled(enabled);
            userRepository.save(user);
        }
        return new ConnectStatusResponse(true, enabled);
    }

    /** Webhook {@code account.updated}: Stripe activó o desactivó los cobros de una cuenta. */
    public void onAccountUpdated(String accountId, boolean payoutsEnabled) {
        userRepository.findByStripeAccountId(accountId).ifPresent(user -> {
            user.setStripePayoutsEnabled(payoutsEnabled);
            userRepository.save(user);
        });
    }

    /**
     * Perfil público del vendedor ({@code /users/:id} en el frontend), que Stripe acepta como web
     * del negocio. En desarrollo devuelve {@code null}: Stripe rechaza las URLs locales y con la
     * descripción de la actividad ya basta.
     */
    String publicProfileUrl(String userId) {
        String host = java.net.URI.create(frontendUrl).getHost();
        if (host == null || host.equals("localhost") || host.equals("127.0.0.1") || host.endsWith(".localhost")) {
            return null;
        }
        return frontendUrl.replaceAll("/+$", "") + "/users/" + userId;
    }

    private User findUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
