package com.dev.marketplace.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.marketplace.api.request.dto.ReserveTransactionRequest;
import com.dev.marketplace.api.request.dto.ShipTransactionRequest;
import com.dev.marketplace.api.response.dto.PaymentIntentResponse;
import com.dev.marketplace.api.response.dto.TransactionResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/reserve")
    public ResponseEntity<TransactionResponse> reserve(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReserveTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.reserve(principal.getUserId(), request));
    }

    // Solo el comprador: secreto del PaymentIntent para pagar con tarjeta (o reintentar el pago).
    @PostMapping("/{id}/payment-intent")
    public ResponseEntity<PaymentIntentResponse> paymentIntent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.paymentIntent(id, principal.getUserId()));
    }

    // Solo el vendedor: acepta una compra con tarjeta ya pagada (plazo de 24 h).
    @PostMapping("/{id}/accept")
    public ResponseEntity<TransactionResponse> accept(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.accept(id, principal.getUserId()));
    }

    // Solo el vendedor: registra el envío con su número de seguimiento.
    @PostMapping("/{id}/ship")
    public ResponseEntity<TransactionResponse> ship(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody ShipTransactionRequest request) {
        return ResponseEntity.ok(transactionService.ship(id, principal.getUserId(), request.trackingNumber()));
    }

    // Solo el comprador: confirma que recibió el artículo y se libera el pago al vendedor.
    @PostMapping("/{id}/received")
    public ResponseEntity<TransactionResponse> received(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.confirmReceipt(id, principal.getUserId()));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<TransactionResponse> confirm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.confirm(id, principal.getUserId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.cancel(id, principal.getUserId()));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<TransactionResponse>> getPurchases(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(transactionService.getPurchases(principal.getUserId()));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<TransactionResponse>> getSales(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(transactionService.getSales(principal.getUserId()));
    }

}
