package com.dev.marketplace.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Transaction;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<Transaction> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    boolean existsByListingIdAndStatus(String listingId, String status);

    boolean existsByListingIdAndStatusAndSellerIdAndBuyerId(String listingId, String status, String sellerId, String buyerId);

    /**
     * Busca la transacción asociada a un PaymentIntent de Stripe (para los eventos del webhook).
     *
     * @param paymentIntentId id del PaymentIntent ({@code pi_...})
     * @return la transacción, si existe
     */
    Optional<Transaction> findByPaymentIntentId(String paymentIntentId);

    /** Transacciones con tarjeta cuyo pago no se completó a tiempo. */
    List<Transaction> findByStatusAndPaymentExpiresAtBefore(String status, Instant now);

    /** Transacciones pagadas que el vendedor no aceptó en el plazo de 24 h. */
    List<Transaction> findByStatusAndAcceptDeadlineBefore(String status, Instant now);

    /** Transacciones aceptadas con envío que el vendedor no envió en el plazo. */
    List<Transaction> findByStatusAndShipDeadlineBefore(String status, Instant now);

    /** Transacciones cuya entrega se da por confirmada automáticamente. */
    List<Transaction> findByStatusAndAutoConfirmAtBefore(String status, Instant now);
}
