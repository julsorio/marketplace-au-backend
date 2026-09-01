package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Transaction;

/**
 * Repositorio Spring Data MongoDB para el acceso a {@link Transaction}.
 */
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    /**
     * Busca las transacciones en las que el usuario indicado es el comprador, ordenadas de más
     * reciente a más antigua.
     *
     * @param buyerId id del usuario comprador
     * @return lista de transacciones del comprador
     */
    List<Transaction> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    /**
     * Busca las transacciones en las que el usuario indicado es el vendedor, ordenadas de más
     * reciente a más antigua.
     *
     * @param sellerId id del usuario vendedor
     * @return lista de transacciones del vendedor
     */
    List<Transaction> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    /**
     * Comprueba si existe alguna transacción para el listing indicado en un estado concreto (por
     * ejemplo, para saber si un anuncio ya tiene una reserva activa).
     *
     * @param listingId id del anuncio
     * @param status estado de la transacción a comprobar
     * @return {@code true} si existe alguna transacción que cumpla ambas condiciones
     */
    boolean existsByListingIdAndStatus(String listingId, String status);

    /**
     * Comprueba si existe una transacción para el listing indicado, en un estado concreto, entre un
     * vendedor y un comprador determinados. Se usa para verificar si dos usuarios completaron una
     * transacción confirmada sobre un listing antes de permitirles dejarse una reseña mutuamente.
     *
     * @param listingId id del anuncio
     * @param status estado de la transacción a comprobar
     * @param sellerId id del vendedor
     * @param buyerId id del comprador
     * @return {@code true} si existe una transacción que cumpla todas las condiciones
     */
    boolean existsByListingIdAndStatusAndSellerIdAndBuyerId(String listingId, String status, String sellerId, String buyerId);
}
