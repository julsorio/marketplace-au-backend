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
import com.dev.marketplace.api.response.dto.TransactionResponse;
import com.dev.marketplace.api.security.UserPrincipal;
import com.dev.marketplace.api.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Expone los endpoints REST para gestionar transacciones: reserva, confirmación y cancelación de
 * compraventas, y consulta del historial de compras y ventas del usuario autenticado.
 */
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    /**
     * Reserva un anuncio creando una transacción pendiente.
     *
     * @param principal usuario autenticado que realiza la reserva
     * @param request datos de la reserva (listing, comprador, importe y método de pago)
     * @return 201 Created con la transacción creada
     */
    @PostMapping("/reserve")
    public ResponseEntity<TransactionResponse> reserve(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReserveTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.reserve(principal.getUserId(), request));
    }

    /**
     * Confirma manualmente el pago en persona de una transacción pendiente.
     *
     * @param principal usuario autenticado que confirma la transacción (debe ser el vendedor)
     * @param id id de la transacción a confirmar
     * @return 200 OK con la transacción confirmada
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<TransactionResponse> confirm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.confirm(id, principal.getUserId()));
    }

    /**
     * Cancela una transacción pendiente.
     *
     * @param principal usuario autenticado que cancela la transacción (vendedor o comprador)
     * @param id id de la transacción a cancelar
     * @return 200 OK con la transacción cancelada
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.cancel(id, principal.getUserId()));
    }

    /**
     * Obtiene el historial de compras del usuario autenticado.
     *
     * @param principal usuario autenticado
     * @return 200 OK con la lista de transacciones en las que el usuario es comprador
     */
    @GetMapping("/purchases")
    public ResponseEntity<List<TransactionResponse>> getPurchases(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(transactionService.getPurchases(principal.getUserId()));
    }

    /**
     * Obtiene el historial de ventas del usuario autenticado.
     *
     * @param principal usuario autenticado
     * @return 200 OK con la lista de transacciones en las que el usuario es vendedor
     */
    @GetMapping("/sales")
    public ResponseEntity<List<TransactionResponse>> getSales(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(transactionService.getSales(principal.getUserId()));
    }

}
