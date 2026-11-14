package com.dev.marketplace.api.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Revisa periódicamente los plazos de las transacciones con tarjeta (pago, aceptación, envío y
 * confirmación automática). Ver {@link TransactionService#processDeadlines}.
 */
@Component
@RequiredArgsConstructor
public class TransactionScheduler {
    private final TransactionService transactionService;

    @Scheduled(fixedDelayString = "${marketplace.scheduler.delay-ms:60000}")
    public void processDeadlines() {
        transactionService.processDeadlines(Instant.now());
    }
}
