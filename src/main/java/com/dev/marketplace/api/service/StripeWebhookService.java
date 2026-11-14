package com.dev.marketplace.api.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.model.ProcessedStripeEvent;
import com.dev.marketplace.api.repository.ProcessedStripeEventRepository;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;

import lombok.RequiredArgsConstructor;

/**
 * Procesa los eventos que Stripe envía al webhook. Es la única vía por la que una transacción con
 * tarjeta pasa a pagada: el resultado que ve el navegador al pagar no cuenta.
 * <p>
 * Si el procesamiento falla, la excepción llega a Stripe como 500 y Stripe reintenta el evento más
 * tarde; solo se marca como procesado cuando termina bien.
 */
@Service
@RequiredArgsConstructor
public class StripeWebhookService {
    private final StripeGateway stripeGateway;
    private final TransactionService transactionService;
    private final ConnectService connectService;
    private final ProcessedStripeEventRepository processedEventRepository;

    public void handle(String payload, String signatureHeader) {
        Event event = stripeGateway.constructEvent(payload, signatureHeader);

        if (processedEventRepository.existsById(event.getId())) {
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) stripeGateway.eventObject(event);
                transactionService.onPaymentSucceeded(intent.getId(), intent.getLatestCharge());
            }
            case "payment_intent.canceled" -> {
                PaymentIntent intent = (PaymentIntent) stripeGateway.eventObject(event);
                transactionService.onPaymentCanceled(intent.getId());
            }
            case "account.updated" -> {
                Account account = (Account) stripeGateway.eventObject(event);
                connectService.onAccountUpdated(account.getId(), stripeGateway.transfersActive(account));
            }
            default -> {
                // Otros eventos (p. ej. payment_intent.payment_failed: el comprador puede reintentar
                // con el mismo PaymentIntent) no cambian nada en el marketplace.
            }
        }

        processedEventRepository.save(new ProcessedStripeEvent(event.getId(), event.getType(), Instant.now()));
    }
}
