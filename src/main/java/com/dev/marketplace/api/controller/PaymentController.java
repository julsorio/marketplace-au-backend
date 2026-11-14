package com.dev.marketplace.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.response.dto.ConnectStatusResponse;
import com.dev.marketplace.api.response.dto.OnboardingLinkResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.ConnectService;
import com.dev.marketplace.api.service.StripeWebhookService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints de pagos con Stripe: alta del vendedor en Stripe Connect y webhook de eventos.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final ConnectService connectService;
    private final StripeWebhookService webhookService;

    /**
     * Devuelve el enlace a la página de alta de Stripe para que el usuario pueda cobrar con tarjeta.
     */
    @PostMapping("/connect/onboarding-link")
    public ResponseEntity<OnboardingLinkResponse> onboardingLink(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(connectService.createOnboardingLink(principal.getUserId()));
    }

    /**
     * Estado del alta del usuario autenticado en Stripe Connect.
     */
    @GetMapping("/connect/status")
    public ResponseEntity<ConnectStatusResponse> status(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(connectService.getStatus(principal.getUserId()));
    }

    /**
     * Webhook de Stripe. Público (lo llama Stripe, sin JWT): la autenticidad se comprueba con la
     * firma de la cabecera {@code Stripe-Signature}, para lo que hace falta el body tal cual llega.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        webhookService.handle(payload, signature);
        return ResponseEntity.ok().build();
    }
}
