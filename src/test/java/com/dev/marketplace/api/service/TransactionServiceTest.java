package com.dev.marketplace.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.dev.marketplace.api.exceptions.InvalidListingStateException;
import com.dev.marketplace.api.exceptions.SellerNotOnboardedException;
import com.dev.marketplace.api.model.Listing;
import com.dev.marketplace.api.model.Price;
import com.dev.marketplace.api.model.Transaction;
import com.dev.marketplace.api.model.TransactionStatus;
import com.dev.marketplace.api.model.User;
import com.dev.marketplace.api.repository.ListingRepository;
import com.dev.marketplace.api.repository.TransactionRepository;
import com.dev.marketplace.api.repository.UserRepository;
import com.dev.marketplace.api.request.dto.ReserveTransactionRequest;
import com.dev.marketplace.api.response.dto.TransactionResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {
    private static final String SELLER = "seller-1";
    private static final String BUYER = "buyer-1";
    private static final String LISTING = "listing-1";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StripeGateway stripeGateway;

    private TransactionService service;
    private Listing listing;
    private User seller;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, listingRepository, userRepository, stripeGateway,
                new CommissionPolicy(new BigDecimal("2.5"), 50));

        listing = new Listing();
        listing.setId(LISTING);
        listing.setSellerId(SELLER);
        listing.setStatus("active");
        listing.setDeliveryMethod("both");
        listing.setPrice(new Price(120.0, "AUD", true));

        seller = new User();
        seller.setId(SELLER);
        seller.setStripeAccountId("acct_1");
        seller.setStripePayoutsEnabled(true);

        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(listing));
        when(userRepository.findById(SELLER)).thenReturn(Optional.of(seller));
        when(stripeGateway.isConfigured()).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId("tx-1");
            }
            return t;
        });
    }

    @Test
    void reservaConTarjetaUsaElPrecioDelAnuncioYCreaElPaymentIntent() {
        when(stripeGateway.createPaymentIntent(12000L, "AUD", "tx-1")).thenReturn("pi_1");

        TransactionResponse response = service.reserve(BUYER,
                new ReserveTransactionRequest(LISTING, null, "card", "shipping"));

        assertEquals(TransactionStatus.AWAITING_PAYMENT, response.status());
        assertEquals(120.0, response.amount());
        assertEquals(3.5, response.platformFee());
        assertNotNull(response.paymentExpiresAt());
        assertEquals("reserved", listing.getStatus());
    }

    @Test
    void elVendedorNoPuedeIniciarUnaCompraConTarjeta() {
        assertThrows(InvalidListingStateException.class, () -> service.reserve(SELLER,
                new ReserveTransactionRequest(LISTING, BUYER, "card", "in_person")));
    }

    @Test
    void noSePuedePagarConTarjetaSiElVendedorNoCompletoElAlta() {
        seller.setStripePayoutsEnabled(false);

        assertThrows(SellerNotOnboardedException.class, () -> service.reserve(BUYER,
                new ReserveTransactionRequest(LISTING, null, "card", "shipping")));
        verify(stripeGateway, never()).createPaymentIntent(anyLong(), anyString(), anyString());
    }

    @Test
    void elEfectivoSoloAdmiteEntregaEnPersona() {
        assertThrows(InvalidListingStateException.class, () -> service.reserve(BUYER,
                new ReserveTransactionRequest(LISTING, null, "in_person", "shipping")));
    }

    @Test
    void elWebhookDePagoAbreElPlazoDeAceptacion() {
        Transaction t = cardTransaction(TransactionStatus.AWAITING_PAYMENT);
        when(transactionRepository.findByPaymentIntentId("pi_1")).thenReturn(Optional.of(t));

        service.onPaymentSucceeded("pi_1", "ch_1");

        assertEquals(TransactionStatus.AWAITING_ACCEPTANCE, t.getStatus());
        assertEquals("ch_1", t.getChargeId());
        assertNotNull(t.getAcceptDeadline());
    }

    @Test
    void siElVendedorRechazaSeReembolsaYElAnuncioVuelveAEstarActivo() {
        Transaction t = cardTransaction(TransactionStatus.AWAITING_ACCEPTANCE);
        listing.setStatus("reserved");
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(t));
        when(stripeGateway.refund("pi_1", "tx-1")).thenReturn("re_1");

        TransactionResponse response = service.cancel("tx-1", SELLER);

        assertEquals(TransactionStatus.CANCELLED, response.status());
        assertEquals("re_1", t.getRefundId());
        assertEquals("active", listing.getStatus());
    }

    @Test
    void elCompradorNoPuedeCancelarUnaCompraYaPagada() {
        Transaction t = cardTransaction(TransactionStatus.ACCEPTED);
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(t));

        assertThrows(InvalidListingStateException.class, () -> service.cancel("tx-1", BUYER));
        verify(stripeGateway, never()).refund(anyString(), anyString());
    }

    @Test
    void trasEnviarYaNoSePuedeCancelar() {
        Transaction t = cardTransaction(TransactionStatus.SHIPPED);
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(t));

        assertThrows(InvalidListingStateException.class, () -> service.cancel("tx-1", SELLER));
    }

    @Test
    void alConfirmarLaRecepcionSeTransfiereElImporteMenosLaComision() {
        Transaction t = cardTransaction(TransactionStatus.SHIPPED);
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(t));
        when(stripeGateway.transfer(11650L, "AUD", "acct_1", "ch_1", "tx-1")).thenReturn("tr_1");

        TransactionResponse response = service.confirmReceipt("tx-1", BUYER);

        assertEquals(TransactionStatus.CONFIRMED, response.status());
        assertEquals("tr_1", t.getTransferId());
        assertEquals("sold", listing.getStatus());
    }

    @Test
    void aceptarConEnvioFijaCincoDiasDesdeElPago() {
        Transaction t = cardTransaction(TransactionStatus.AWAITING_ACCEPTANCE);
        Instant paidAt = Instant.parse("2026-10-20T10:00:00Z");
        t.setPaidAt(paidAt);
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(t));

        service.accept("tx-1", SELLER);

        assertEquals(TransactionStatus.ACCEPTED, t.getStatus());
        assertEquals(paidAt.plus(TransactionService.SHIP_WINDOW), t.getShipDeadline());
    }

    @Test
    void losPlazosVencidosReembolsanOConfirman() {
        Instant now = Instant.now();
        Transaction notAccepted = cardTransaction(TransactionStatus.AWAITING_ACCEPTANCE);
        Transaction shipped = cardTransaction(TransactionStatus.SHIPPED);
        shipped.setId("tx-2");
        when(transactionRepository.findByStatusAndAcceptDeadlineBefore(TransactionStatus.AWAITING_ACCEPTANCE, now))
                .thenReturn(List.of(notAccepted));
        when(transactionRepository.findByStatusAndAutoConfirmAtBefore(TransactionStatus.SHIPPED, now))
                .thenReturn(List.of(shipped));
        when(stripeGateway.refund("pi_1", "tx-1")).thenReturn("re_1");
        when(stripeGateway.transfer(anyLong(), anyString(), anyString(), anyString(), eq("tx-2"))).thenReturn("tr_2");

        service.processDeadlines(now);

        assertEquals(TransactionStatus.CANCELLED, notAccepted.getStatus());
        assertEquals(TransactionStatus.CONFIRMED, shipped.getStatus());
    }

    private Transaction cardTransaction(String status) {
        Transaction t = new Transaction();
        t.setId("tx-1");
        t.setListingId(LISTING);
        t.setSellerId(SELLER);
        t.setBuyerId(BUYER);
        t.setAmount(120.0);
        t.setCurrency("AUD");
        t.setPlatformFee(3.5);
        t.setPaymentMethod("card");
        t.setDeliveryType("shipping");
        t.setStatus(status);
        t.setPaymentIntentId("pi_1");
        t.setChargeId("ch_1");
        return t;
    }
}
