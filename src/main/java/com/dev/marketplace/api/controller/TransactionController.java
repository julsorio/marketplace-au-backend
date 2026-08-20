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
