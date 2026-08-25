package com.dev.marketplace.api.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.InvalidListingStateException;
import com.dev.marketplace.api.exceptions.ListingNotFoundException;
import com.dev.marketplace.api.exceptions.TransactionNotFoundException;
import com.dev.marketplace.api.exceptions.UnauthorizedTransactionAccessException;
import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.model.Transaction;
import com.dev.marketplace.api.repository.ListingRepository;
import com.dev.marketplace.api.repository.TransactionRepository;
import com.dev.marketplace.api.request.dto.ReserveTransactionRequest;
import com.dev.marketplace.api.response.dto.TransactionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;

    // requesterId es quien hace la petición, no necesariamente el vendedor: puede reservar
    // tanto el vendedor (eligiendo a qué comprador, típicamente tras negociar por chat) como
    // el propio comprador haciendo clic en "Comprar" directamente desde el anuncio. Se infiere
    // el rol comparando requesterId con el dueño del anuncio; si no es el vendedor, se asume
    // que es el comprador y se ignora cualquier buyerId distinto que llegue en el body.
    public TransactionResponse reserve(String requesterId, ReserveTransactionRequest request) {
        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ListingNotFoundException(request.listingId()));

        if (!"active".equals(listing.getStatus())) {
            throw new InvalidListingStateException(
                    "El anuncio debe estar activo para reservarlo. Estado actual: " + listing.getStatus());
        }

        boolean isSeller = listing.getSellerId().equals(requesterId);
        String sellerId = listing.getSellerId();
        String buyerId = isSeller ? request.buyerId() : requesterId;

        if (sellerId.equals(buyerId)) {
            throw new InvalidListingStateException("El vendedor no puede ser también el comprador");
        }

        Transaction transaction = new Transaction();
        transaction.setListingId(request.listingId());
        transaction.setSellerId(sellerId);
        transaction.setBuyerId(buyerId);
        transaction.setAmount(request.amount());
        transaction.setCurrency("AUD");
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setStatus("pending");

        Transaction saved = transactionRepository.save(transaction);

        listing.setStatus("reserved");
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        // Nota: cuando se integre Stripe, aquí se crearía el PaymentIntent
        // si transaction.getPaymentMethod().equals("card"), y se guardaría
        // el paymentIntentId antes de devolver la respuesta.

        return toResponse(saved);
    }

    public TransactionResponse confirm(String id, String requesterId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        if (!transaction.getSellerId().equals(requesterId)) {
            throw new UnauthorizedTransactionAccessException();
        }

        if (!"in_person".equals(transaction.getPaymentMethod())) {
            throw new InvalidListingStateException(
                    "Las transacciones con tarjeta se confirman automáticamente vía webhook, no manualmente");
        }

        if (!"pending".equals(transaction.getStatus())) {
            throw new InvalidListingStateException(
                    "Solo se pueden confirmar transacciones pendientes. Estado actual: " + transaction.getStatus());
        }

        transaction.setStatus("confirmed");
        transaction.setConfirmedAt(Instant.now());
        transactionRepository.save(transaction);

        Listing listing = listingRepository.findById(transaction.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(transaction.getListingId()));
        listing.setStatus("sold");
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        return toResponse(transaction);
    }

    public TransactionResponse cancel(String id, String requesterId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        boolean isParticipant = transaction.getSellerId().equals(requesterId)
                || transaction.getBuyerId().equals(requesterId);

        if (!isParticipant) {
            throw new UnauthorizedTransactionAccessException();
        }

        if (!"pending".equals(transaction.getStatus())) {
            throw new InvalidListingStateException(
                    "Solo se pueden cancelar transacciones pendientes. Estado actual: " + transaction.getStatus());
        }

        transaction.setStatus("cancelled");
        transactionRepository.save(transaction);

        Listing listing = listingRepository.findById(transaction.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(transaction.getListingId()));
        listing.setStatus("active");
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        return toResponse(transaction);
    }

    public List<TransactionResponse> getPurchases(String buyerId) {
        return transactionRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionResponse> getSales(String sellerId) {
        return transactionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getListingId(), t.getSellerId(), t.getBuyerId(),
                t.getAmount(), t.getCurrency(), t.getPaymentMethod(), t.getStatus(),
                t.getCreatedAt(), t.getConfirmedAt());
    }
}
