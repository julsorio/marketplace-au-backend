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

/**
 * Gestiona el ciclo de vida de las transacciones (reserva, confirmación y cancelación de
 * compraventas), coordinando el estado del anuncio (listing) asociado en cada paso.
 * <p>
 * El ciclo de estados del listing es: activo → reservado → vendido/cancelado. En paralelo, la
 * transacción recorre su propio ciclo: pending → confirmed/cancelled.
 * <p>
 * Tanto el vendedor como el comprador pueden iniciar una reserva: el rol de quien hace la petición se
 * infiere comparando el id del solicitante con el dueño del anuncio, no viene indicado explícitamente
 * en la petición.
 * <p>
 * El pago en persona ({@code paymentMethod = "in_person"}) lo confirma manualmente el vendedor
 * mediante {@link #confirm}. El pago con tarjeta ({@code paymentMethod = "card"}) está modelado en el
 * dominio (ver {@link Transaction#getPaymentIntentId()}), pero solo el flujo in_person está
 * implementado: la confirmación automática de pagos con tarjeta vía webhook de Stripe queda
 * pendiente de integrar.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;

    /**
     * Crea una reserva de transacción para un anuncio activo, dejando el anuncio en estado
     * "reservado" y la transacción en estado "pending".
     * <p>
     * {@code requesterId} es quien hace la petición, no necesariamente el vendedor: puede reservar
     * tanto el vendedor (eligiendo a qué comprador, típicamente tras negociar por chat) como el
     * propio comprador haciendo clic en "Comprar" directamente desde el anuncio. El rol se infiere
     * comparando {@code requesterId} con el dueño del anuncio; si no es el vendedor, se asume que es
     * el comprador y se ignora cualquier {@code buyerId} distinto que llegue en el body.
     *
     * @param requesterId id del usuario autenticado que realiza la petición de reserva
     * @param request datos de la reserva: listing, comprador (si quien reserva es el vendedor),
     *        importe y método de pago
     * @return la transacción creada, en estado "pending"
     * @throws ListingNotFoundException si no existe un anuncio con el listingId indicado
     * @throws InvalidListingStateException si el anuncio no está activo, o si el vendedor y el
     *         comprador resultantes coinciden
     */
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

    /**
     * Confirma manualmente una transacción pendiente pagada en persona, marcando el anuncio asociado
     * como "vendido".
     * <p>
     * Solo el vendedor puede confirmar la transacción, y solo está implementado el flujo de pago en
     * persona: las transacciones con tarjeta se confirmarían automáticamente vía webhook de Stripe
     * (todavía sin implementar), no manualmente a través de este método.
     *
     * @param id id de la transacción a confirmar
     * @param requesterId id del usuario autenticado que solicita la confirmación
     * @return la transacción actualizada, en estado "confirmed"
     * @throws TransactionNotFoundException si no existe una transacción con ese id
     * @throws UnauthorizedTransactionAccessException si el solicitante no es el vendedor de la
     *         transacción
     * @throws InvalidListingStateException si el método de pago de la transacción no es "in_person",
     *         o si la transacción no está en estado "pending"
     * @throws ListingNotFoundException si el anuncio asociado a la transacción ya no existe
     */
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

    /**
     * Cancela una transacción pendiente y repone el anuncio asociado a estado "activo".
     * <p>
     * A diferencia de la confirmación (reservada al vendedor), la cancelación la puede iniciar
     * cualquiera de los dos participantes de la transacción, tanto el vendedor como el comprador.
     *
     * @param id id de la transacción a cancelar
     * @param requesterId id del usuario autenticado que solicita la cancelación
     * @return la transacción actualizada, en estado "cancelled"
     * @throws TransactionNotFoundException si no existe una transacción con ese id
     * @throws UnauthorizedTransactionAccessException si el solicitante no es ni el vendedor ni el
     *         comprador de la transacción
     * @throws InvalidListingStateException si la transacción no está en estado "pending"
     * @throws ListingNotFoundException si el anuncio asociado a la transacción ya no existe
     */
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

    /**
     * Obtiene el historial de compras de un usuario (transacciones en las que es el comprador),
     * ordenado de más reciente a más antigua.
     *
     * @param buyerId id del usuario comprador
     * @return lista de transacciones del comprador
     */
    public List<TransactionResponse> getPurchases(String buyerId) {
        return transactionRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Obtiene el historial de ventas de un usuario (transacciones en las que es el vendedor),
     * ordenado de más reciente a más antigua.
     *
     * @param sellerId id del usuario vendedor
     * @return lista de transacciones del vendedor
     */
    public List<TransactionResponse> getSales(String sellerId) {
        return transactionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Convierte una entidad {@link Transaction} en su DTO de respuesta {@link TransactionResponse}.
     *
     * @param t transacción a convertir
     * @return el DTO de respuesta correspondiente
     */
    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getListingId(), t.getSellerId(), t.getBuyerId(),
                t.getAmount(), t.getCurrency(), t.getPaymentMethod(), t.getStatus(),
                t.getCreatedAt(), t.getConfirmedAt());
    }
}
