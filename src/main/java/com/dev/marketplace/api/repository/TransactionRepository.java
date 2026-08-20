package com.dev.marketplace.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dev.marketplace.api.model.Transaction;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<Transaction> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    boolean existsByListingIdAndStatus(String listingId, String status);

    boolean existsByListingIdAndStatusAndSellerIdAndBuyerId(String listingId, String status, String sellerId, String buyerId);
}
