package com.dev.marketplace.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.dev.marketplace.api.exceptions.InvalidListingStateException;
import com.dev.marketplace.api.exceptions.ListingNotFoundException;
import com.dev.marketplace.api.exceptions.PaymentProviderException;
import com.dev.marketplace.api.exceptions.SellerNotOnboardedException;
import com.dev.marketplace.api.exceptions.TransactionNotFoundException;
import com.dev.marketplace.api.exceptions.UnauthorizedTransactionAccessException;
import com.dev.marketplace.api.exceptions.UserNotFoundException;
import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.model.Transaction;
import com.dev.marketplace.api.model.TransactionStatus;
import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.ListingRepository;
import com.dev.marketplace.api.repository.TransactionRepository;
import com.dev.marketplace.api.repository.UserRepository;
import com.dev.marketplace.api.request.dto.ReserveTransactionRequest;
import com.dev.marketplace.api.response.dto.PaymentIntentResponse;
import com.dev.marketplace.api.response.dto.TransactionResponse;
import com.dev.marketplace.api.util.Money;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestiona el ciclo de vida de las transacciones, coordinando el estado del anuncio (listing)
 * asociado en cada paso: activo → reservado → vendido, o de vuelta a activo si se cancela.
 * <p>
 * Pago en persona ({@code paymentMethod = "in_person"}): {@code pending} → el vendedor confirma
 * manualmente con {@link #confirm} → {@code confirmed}.
 * <p>
 * Pago con tarjeta ({@code paymentMethod = "card"}), siempre iniciado por el comprador:
 * <ol>
 * <li>{@link #reserve} crea el PaymentIntent: {@code awaiting_payment}, con 30 minutos para pagar.</li>
 * <li>El webhook de Stripe confirma el cobro ({@link #onPaymentSucceeded}): {@code awaiting_acceptance}.
 * El dinero queda en la cuenta de la plataforma.</li>
 * <li>El vendedor acepta en 24 h ({@link #accept}): {@code accepted}. Con envío tiene 5 días desde el
 * pago para registrar el número de seguimiento ({@link #ship}): {@code shipped}.</li>
 * <li>El comprador confirma que lo recibió ({@link #confirmReceipt}), o pasan 14 días: se transfiere
 * al vendedor el importe menos la comisión y la transacción queda {@code confirmed}.</li>
 * </ol>
 * Cualquier cancelación tras el pago reembolsa íntegramente al comprador. Los plazos los vigila
 * {@link TransactionScheduler} llamando a {@link #processDeadlines}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    static final Duration PAYMENT_WINDOW = Duration.ofMinutes(30);
    static final Duration ACCEPT_WINDOW = Duration.ofHours(24);
    static final Duration SHIP_WINDOW = Duration.ofDays(5);
    static final Duration AUTO_CONFIRM_WINDOW = Duration.ofDays(14);

    private static final String CARD = "card";
    private static final String IN_PERSON = "in_person";
    private static final String SHIPPING = "shipping";

    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final StripeGateway stripeGateway;
    private final CommissionPolicy commissionPolicy;

    /**
     * Crea una reserva de transacción para un anuncio activo y deja el anuncio en estado "reservado".
     * <p>
     * {@code requesterId} es quien hace la petición, no necesariamente el vendedor: puede reservar
     * tanto el vendedor (eligiendo a qué comprador, típicamente tras negociar por chat) como el
     * propio comprador haciendo clic en "Comprar" directamente desde el anuncio. El rol se infiere
     * comparando {@code requesterId} con el dueño del anuncio; si no es el vendedor, se asume que es
     * el comprador y se ignora cualquier {@code buyerId} distinto que llegue en el body.
     * <p>
     * El importe es siempre el precio actual del anuncio. Con tarjeta solo puede reservar el
     * comprador, y el vendedor debe tener completada el alta en Stripe Connect.
     *
     * @param requesterId id del usuario autenticado que realiza la petición de reserva
     * @param request datos de la reserva: listing, comprador (si quien reserva es el vendedor),
     *        método de pago y tipo de entrega
     * @return la transacción creada, en estado "pending" (efectivo) o "awaiting_payment" (tarjeta)
     * @throws ListingNotFoundException si no existe un anuncio con el listingId indicado
     * @throws InvalidListingStateException si el anuncio no está activo, la combinación de pago y
     *         entrega no es válida, o el vendedor y el comprador resultantes coinciden
     * @throws SellerNotOnboardedException si se paga con tarjeta y el vendedor no puede cobrarla
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

        if (buyerId == null || buyerId.isBlank()) {
            throw new InvalidListingStateException("Indica el comprador de la reserva");
        }
        if (sellerId.equals(buyerId)) {
            throw new InvalidListingStateException("El vendedor no puede ser también el comprador");
        }

        validateDelivery(listing, request);

        boolean card = CARD.equals(request.paymentMethod());
        if (card) {
            if (isSeller) {
                throw new InvalidListingStateException(
                        "Las compras con tarjeta las inicia el comprador desde el anuncio");
            }
            if (!stripeGateway.isConfigured()) {
                throw new PaymentProviderException("Los pagos con tarjeta no están disponibles en este momento");
            }
            User seller = userRepository.findById(sellerId).orElseThrow(() -> new UserNotFoundException(sellerId));
            if (seller.getStripeAccountId() == null || !seller.isStripePayoutsEnabled()) {
                throw new SellerNotOnboardedException();
            }
        }

        double amount = listing.getPrice().amount();
        String currency = listing.getPrice().currency() != null ? listing.getPrice().currency() : "AUD";

        Transaction transaction = new Transaction();
        transaction.setListingId(request.listingId());
        transaction.setSellerId(sellerId);
        transaction.setBuyerId(buyerId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setDeliveryType(request.deliveryType());

        if (card) {
            transaction.setStatus(TransactionStatus.AWAITING_PAYMENT);
            transaction.setPlatformFee(commissionPolicy.feeFor(amount));
            transaction.setPaymentExpiresAt(Instant.now().plus(PAYMENT_WINDOW));
        } else {
            transaction.setStatus(TransactionStatus.PENDING);
        }

        Transaction saved = transactionRepository.save(transaction);

        if (card) {
            // El id de la transacción hace falta para el PaymentIntent (transfer_group y clave de
            // idempotencia), así que se guarda primero y se borra si Stripe falla.
            try {
                saved.setPaymentIntentId(
                        stripeGateway.createPaymentIntent(Money.toCents(amount), currency, saved.getId()));
                saved = transactionRepository.save(saved);
            } catch (PaymentProviderException e) {
                transactionRepository.delete(saved);
                throw e;
            }
        }

        listing.setStatus("reserved");
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        return toResponse(saved);
    }

    /**
     * Devuelve al comprador el secreto del PaymentIntent para pagar (o reintentar el pago) desde el
     * frontend.
     */
    public PaymentIntentResponse paymentIntent(String id, String requesterId) {
        Transaction transaction = findTransaction(id);
        requireBuyer(transaction, requesterId);
        requireStatus(transaction, TransactionStatus.AWAITING_PAYMENT, "La transacción no está pendiente de pago");

        return new PaymentIntentResponse(stripeGateway.getClientSecret(transaction.getPaymentIntentId()));
    }

    /**
     * El vendedor acepta una compra ya pagada. Con envío, arranca el plazo de 5 días (contados desde
     * el pago) para enviar; con entrega en persona, el de confirmación automática.
     */
    public TransactionResponse accept(String id, String requesterId) {
        Transaction transaction = findTransaction(id);
        requireSeller(transaction, requesterId);
        requireStatus(transaction, TransactionStatus.AWAITING_ACCEPTANCE,
                "Solo se pueden aceptar compras pagadas pendientes de aceptación");

        Instant now = Instant.now();
        transaction.setAcceptedAt(now);
        transaction.setStatus(TransactionStatus.ACCEPTED);
        if (SHIPPING.equals(transaction.getDeliveryType())) {
            Instant paidAt = transaction.getPaidAt() != null ? transaction.getPaidAt() : now;
            transaction.setShipDeadline(paidAt.plus(SHIP_WINDOW));
        } else {
            transaction.setAutoConfirmAt(now.plus(AUTO_CONFIRM_WINDOW));
        }

        return toResponse(transactionRepository.save(transaction));
    }

    /**
     * El vendedor registra el envío con su número de seguimiento. A partir de aquí ya no puede
     * cancelar, y arranca el plazo de confirmación automática.
     */
    public TransactionResponse ship(String id, String requesterId, String trackingNumber) {
        Transaction transaction = findTransaction(id);
        requireSeller(transaction, requesterId);
        requireStatus(transaction, TransactionStatus.ACCEPTED, "Solo se pueden enviar compras aceptadas");
        if (!SHIPPING.equals(transaction.getDeliveryType())) {
            throw new InvalidListingStateException("Esta compra es con entrega en persona, no con envío");
        }

        Instant now = Instant.now();
        transaction.setTrackingNumber(trackingNumber.trim());
        transaction.setShippedAt(now);
        transaction.setStatus(TransactionStatus.SHIPPED);
        transaction.setAutoConfirmAt(now.plus(AUTO_CONFIRM_WINDOW));

        return toResponse(transactionRepository.save(transaction));
    }

    /**
     * El comprador confirma que recibió el artículo: se transfiere el dinero al vendedor y la venta
     * queda confirmada.
     */
    public TransactionResponse confirmReceipt(String id, String requesterId) {
        Transaction transaction = findTransaction(id);
        requireBuyer(transaction, requesterId);

        boolean shipped = TransactionStatus.SHIPPED.equals(transaction.getStatus());
        boolean handedOver = TransactionStatus.ACCEPTED.equals(transaction.getStatus())
                && IN_PERSON.equals(transaction.getDeliveryType());
        if (!CARD.equals(transaction.getPaymentMethod()) || !(shipped || handedOver)) {
            throw new InvalidListingStateException("Esta compra todavía no se puede dar por recibida");
        }

        return toResponse(complete(transaction));
    }

    /**
     * Confirma manualmente una transacción pendiente pagada en persona, marcando el anuncio asociado
     * como "vendido".
     * <p>
     * Solo el vendedor puede confirmar la transacción. Las transacciones con tarjeta siguen su
     * propio flujo (aceptación, envío y confirmación de recepción), no pasan por este método.
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
        Transaction transaction = findTransaction(id);
        requireSeller(transaction, requesterId);

        if (!IN_PERSON.equals(transaction.getPaymentMethod())) {
            throw new InvalidListingStateException(
                    "Las transacciones con tarjeta se confirman cuando el comprador recibe el artículo");
        }
        requireStatus(transaction, TransactionStatus.PENDING,
                "Solo se pueden confirmar transacciones pendientes. Estado actual: " + transaction.getStatus());

        transaction.setStatus(TransactionStatus.CONFIRMED);
        transaction.setConfirmedAt(Instant.now());
        transactionRepository.save(transaction);
        markListing(transaction, "sold");

        return toResponse(transaction);
    }

    /**
     * Cancela una transacción y repone el anuncio asociado a estado "activo".
     * <p>
     * En efectivo, cualquiera de los dos participantes puede cancelar mientras está pendiente. Con
     * tarjeta, antes de pagar puede cancelar cualquiera de los dos; una vez pagada, solo el vendedor
     * (rechazar la compra o anular el envío), y solo hasta registrar el número de seguimiento. El
     * pago se reembolsa íntegramente.
     *
     * @param id id de la transacción a cancelar
     * @param requesterId id del usuario autenticado que solicita la cancelación
     * @return la transacción actualizada, en estado "cancelled"
     * @throws TransactionNotFoundException si no existe una transacción con ese id
     * @throws UnauthorizedTransactionAccessException si el solicitante no es ni el vendedor ni el
     *         comprador de la transacción
     * @throws InvalidListingStateException si la transacción ya no se puede cancelar
     * @throws ListingNotFoundException si el anuncio asociado a la transacción ya no existe
     */
    public TransactionResponse cancel(String id, String requesterId) {
        Transaction transaction = findTransaction(id);

        boolean isSeller = transaction.getSellerId().equals(requesterId);
        boolean isBuyer = transaction.getBuyerId().equals(requesterId);
        if (!isSeller && !isBuyer) {
            throw new UnauthorizedTransactionAccessException();
        }

        if (!CARD.equals(transaction.getPaymentMethod())) {
            requireStatus(transaction, TransactionStatus.PENDING,
                    "Solo se pueden cancelar transacciones pendientes. Estado actual: " + transaction.getStatus());
            return toResponse(cancelAndRelease(transaction));
        }

        switch (transaction.getStatus()) {
            case TransactionStatus.AWAITING_PAYMENT -> {
                if (!stripeGateway.cancelPaymentIntent(transaction.getPaymentIntentId())) {
                    throw new InvalidListingStateException(
                            "El pago ya se está procesando. Vuelve a intentarlo en unos segundos");
                }
                return toResponse(cancelAndRelease(transaction));
            }
            case TransactionStatus.AWAITING_ACCEPTANCE, TransactionStatus.ACCEPTED -> {
                if (!isSeller) {
                    throw new InvalidListingStateException(
                            "Una vez pagada, solo el vendedor puede cancelar la compra antes del envío");
                }
                return toResponse(refundAndRelease(transaction));
            }
            default -> throw new InvalidListingStateException(
                    "La transacción ya no se puede cancelar. Estado actual: " + transaction.getStatus());
        }
    }

    /**
     * Stripe confirmó el cobro de un PaymentIntent (webhook {@code payment_intent.succeeded}).
     * Si la reserva ya se había cancelado (el pago llegó justo cuando caducaba), se reembolsa.
     */
    public void onPaymentSucceeded(String paymentIntentId, String chargeId) {
        Transaction transaction = transactionRepository.findByPaymentIntentId(paymentIntentId).orElse(null);
        if (transaction == null) {
            log.warn("Pago {} sin transacción asociada", paymentIntentId);
            return;
        }

        if (TransactionStatus.AWAITING_PAYMENT.equals(transaction.getStatus())) {
            Instant now = Instant.now();
            transaction.setChargeId(chargeId);
            transaction.setPaidAt(now);
            transaction.setAcceptDeadline(now.plus(ACCEPT_WINDOW));
            transaction.setStatus(TransactionStatus.AWAITING_ACCEPTANCE);
            transactionRepository.save(transaction);
        } else if (TransactionStatus.CANCELLED.equals(transaction.getStatus()) && transaction.getRefundId() == null) {
            transaction.setChargeId(chargeId);
            transaction.setRefundId(stripeGateway.refund(paymentIntentId, transaction.getId()));
            transactionRepository.save(transaction);
        }
    }

    /**
     * Stripe canceló un PaymentIntent (webhook {@code payment_intent.canceled}).
     */
    public void onPaymentCanceled(String paymentIntentId) {
        transactionRepository.findByPaymentIntentId(paymentIntentId)
                .filter(t -> TransactionStatus.AWAITING_PAYMENT.equals(t.getStatus()))
                .ifPresent(this::cancelAndRelease);
    }

    /**
     * Aplica los plazos de la política de cancelación y de confirmación automática. Cada transacción
     * se procesa por separado: si una falla (p. ej. Stripe no responde), se reintenta en la
     * siguiente ejecución sin bloquear al resto.
     *
     * @param now instante de referencia
     */
    public void processDeadlines(Instant now) {
        forEach(transactionRepository.findByStatusAndPaymentExpiresAtBefore(TransactionStatus.AWAITING_PAYMENT, now),
                t -> {
                    // Si el pago ya está en curso, el webhook lo moverá a awaiting_acceptance.
                    if (stripeGateway.cancelPaymentIntent(t.getPaymentIntentId())) {
                        cancelAndRelease(t);
                    }
                });
        forEach(transactionRepository.findByStatusAndAcceptDeadlineBefore(TransactionStatus.AWAITING_ACCEPTANCE, now),
                this::refundAndRelease);
        forEach(transactionRepository.findByStatusAndShipDeadlineBefore(TransactionStatus.ACCEPTED, now),
                this::refundAndRelease);
        forEach(transactionRepository.findByStatusAndAutoConfirmAtBefore(TransactionStatus.SHIPPED, now),
                this::complete);
        forEach(transactionRepository.findByStatusAndAutoConfirmAtBefore(TransactionStatus.ACCEPTED, now),
                this::complete);
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

    private void validateDelivery(Listing listing, ReserveTransactionRequest request) {
        String method = listing.getDeliveryMethod() != null ? listing.getDeliveryMethod() : IN_PERSON;
        if (!"both".equals(method) && !method.equals(request.deliveryType())) {
            throw new InvalidListingStateException("El anuncio no admite ese tipo de entrega");
        }
        if (IN_PERSON.equals(request.paymentMethod()) && SHIPPING.equals(request.deliveryType())) {
            throw new InvalidListingStateException("El pago en efectivo solo es posible con entrega en persona");
        }
    }

    /** Transfiere al vendedor su parte y da la venta por confirmada. */
    private Transaction complete(Transaction transaction) {
        User seller = userRepository.findById(transaction.getSellerId())
                .orElseThrow(() -> new UserNotFoundException(transaction.getSellerId()));

        long netCents = Money.toCents(transaction.getAmount()) - Money.toCents(transaction.getPlatformFee());
        transaction.setTransferId(stripeGateway.transfer(netCents, transaction.getCurrency(),
                seller.getStripeAccountId(), transaction.getChargeId(), transaction.getId()));
        transaction.setStatus(TransactionStatus.CONFIRMED);
        transaction.setConfirmedAt(Instant.now());
        transactionRepository.save(transaction);
        markListing(transaction, "sold");
        return transaction;
    }

    private Transaction refundAndRelease(Transaction transaction) {
        transaction.setRefundId(stripeGateway.refund(transaction.getPaymentIntentId(), transaction.getId()));
        return cancelAndRelease(transaction);
    }

    private Transaction cancelAndRelease(Transaction transaction) {
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setCancelledAt(Instant.now());
        transactionRepository.save(transaction);
        markListing(transaction, "active");
        return transaction;
    }

    private void markListing(Transaction transaction, String status) {
        Listing listing = listingRepository.findById(transaction.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(transaction.getListingId()));
        listing.setStatus(status);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);
    }

    private void forEach(List<Transaction> transactions, Consumer<Transaction> action) {
        for (Transaction transaction : transactions) {
            try {
                action.accept(transaction);
            } catch (RuntimeException e) {
                log.error("No se pudo aplicar el plazo vencido de la transacción {}", transaction.getId(), e);
            }
        }
    }

    private Transaction findTransaction(String id) {
        return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private void requireSeller(Transaction transaction, String requesterId) {
        if (!transaction.getSellerId().equals(requesterId)) {
            throw new UnauthorizedTransactionAccessException();
        }
    }

    private void requireBuyer(Transaction transaction, String requesterId) {
        if (!transaction.getBuyerId().equals(requesterId)) {
            throw new UnauthorizedTransactionAccessException();
        }
    }

    private void requireStatus(Transaction transaction, String status, String message) {
        if (!status.equals(transaction.getStatus())) {
            throw new InvalidListingStateException(message);
        }
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
                t.getAmount(), t.getCurrency(), t.getPaymentMethod(), t.getDeliveryType(), t.getStatus(),
                t.getPlatformFee(), t.getTrackingNumber(),
                t.getCreatedAt(), t.getPaidAt(), t.getAcceptedAt(), t.getShippedAt(), t.getConfirmedAt(),
                t.getPaymentExpiresAt(), t.getAcceptDeadline(), t.getShipDeadline(), t.getAutoConfirmAt());
    }
}
