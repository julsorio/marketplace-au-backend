package com.dev.marketplace.api.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dev.marketplace.api.exceptions.InvalidWebhookSignatureException;
import com.dev.marketplace.api.exceptions.PaymentProviderException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.StripeClient;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.v2.core.AccountCreateParams;
import com.stripe.param.v2.core.AccountLinkCreateParams;
import com.stripe.param.v2.core.AccountRetrieveParams;

/**
 * Único punto de contacto con la API de Stripe. Devuelve tipos simples (ids, booleanos) para que
 * los servicios de negocio no dependan del SDK y se puedan probar con un mock de esta clase.
 * <p>
 * Modelo de Connect: <i>separate charges and transfers</i>. El cobro queda en la cuenta de la
 * plataforma ({@link #createPaymentIntent}) y el dinero se transfiere al vendedor solo cuando se
 * confirma la entrega ({@link #transfer}), así un reembolso nunca tiene que recuperarlo del vendedor.
 * <p>
 * Cada operación que mueve dinero lleva una clave de idempotencia derivada del id de la transacción:
 * si el webhook, la tarea programada y el usuario disparan la misma acción a la vez, Stripe solo la
 * ejecuta una vez.
 */
@Component
public class StripeGateway {
    private final String secretKey;
    private final List<String> webhookSecrets;
    private StripeClient stripeClient;

    public StripeGateway(@Value("${stripe.secret-key:}") String secretKey,
            @Value("${stripe.webhook-secret:}") String webhookSecrets) {
        this.secretKey = secretKey;
        // Admite varios secretos separados por comas: en producción el endpoint de la cuenta de
        // la plataforma y el de las cuentas conectadas (account.updated) tienen secretos distintos.
        this.webhookSecrets = Arrays.stream(webhookSecrets.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    /**
     * Crea el PaymentIntent del cobro al comprador.
     *
     * @return id del PaymentIntent
     */
    public String createPaymentIntent(long amountCents, String currency, String transactionId) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency.toLowerCase())
                .setTransferGroup(transactionId)
                .putMetadata("transactionId", transactionId)
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build())
                .build();
        try {
            return PaymentIntent.create(params, options("pi-" + transactionId)).getId();
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo iniciar el pago con tarjeta", e);
        }
    }

    public String getClientSecret(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId, options(null)).getClientSecret();
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo recuperar el pago", e);
        }
    }

    /**
     * Cancela un PaymentIntent que todavía no se ha cobrado.
     *
     * @return {@code true} si quedó cancelado (o ya lo estaba); {@code false} si ya está cobrado o
     *         procesándose, en cuyo caso no se puede cancelar
     */
    public boolean cancelPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, options(null));
            String status = intent.getStatus();
            if ("canceled".equals(status)) {
                return true;
            }
            if ("succeeded".equals(status) || "processing".equals(status)) {
                return false;
            }
            intent.cancel(options(null));
            return true;
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo cancelar el pago", e);
        }
    }

    /**
     * Reembolsa íntegramente un pago.
     *
     * @return id del reembolso
     */
    public String refund(String paymentIntentId, String transactionId) {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();
        try {
            return Refund.create(params, options("re-" + transactionId)).getId();
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo reembolsar el pago", e);
        }
    }

    /**
     * Transfiere al vendedor su parte de un cobro ya realizado.
     *
     * @return id de la transferencia
     */
    public String transfer(long amountCents, String currency, String destinationAccountId, String chargeId,
            String transactionId) {
        TransferCreateParams.Builder params = TransferCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency.toLowerCase())
                .setDestination(destinationAccountId)
                .setTransferGroup(transactionId);
        // Atar la transferencia al cobro permite hacerla aunque el saldo de la plataforma todavía
        // no esté disponible.
        if (chargeId != null) {
            params.setSourceTransaction(chargeId);
        }
        try {
            return Transfer.create(params.build(), options("tr-" + transactionId)).getId();
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo transferir el importe al vendedor", e);
        }
    }

    /** Lo que Stripe ve como "actividad" de cada vendedor; evita que le pida una web propia. */
    static final String SELLER_PRODUCT_DESCRIPTION =
            "Venta de artículos de segunda mano entre particulares en Marketplace AU";

    /**
     * Crea la cuenta conectada de un vendedor con Accounts v2 (Stripe ya no admite crear cuentas
     * v1 con {@code type=express}): dashboard Express, la plataforma asume pérdidas y comisiones (lo
     * exige separate charges and transfers) y solo se pide poder recibir transferencias.
     * <p>
     * Se rellena de antemano lo que la plataforma ya sabe para acortar el formulario de alta: el
     * vendedor es un particular (sin ABN ni datos de empresa) y la actividad se describe con
     * {@link #SELLER_PRODUCT_DESCRIPTION} en lugar de pedirle una web.
     *
     * @param profileUrl página pública del vendedor en el marketplace, o {@code null} (Stripe
     *                   rechaza URLs locales, así que en desarrollo no se envía)
     * @return id de la cuenta ({@code acct_...})
     */
    public String createConnectedAccount(String email, String userId, String profileUrl) {
        AccountCreateParams.Defaults.Profile.Builder profile = AccountCreateParams.Defaults.Profile.builder()
                .setProductDescription(SELLER_PRODUCT_DESCRIPTION);
        if (profileUrl != null) {
            profile.setBusinessUrl(profileUrl);
        }
        AccountCreateParams params = AccountCreateParams.builder()
                .setContactEmail(email)
                .setDashboard(AccountCreateParams.Dashboard.EXPRESS)
                .setIdentity(AccountCreateParams.Identity.builder()
                        .setCountry("AU")
                        .setEntityType(AccountCreateParams.Identity.EntityType.INDIVIDUAL)
                        .build())
                .setDefaults(AccountCreateParams.Defaults.builder()
                        .setProfile(profile.build())
                        .setResponsibilities(AccountCreateParams.Defaults.Responsibilities.builder()
                                .setFeesCollector(AccountCreateParams.Defaults.Responsibilities.FeesCollector.APPLICATION)
                                .setLossesCollector(AccountCreateParams.Defaults.Responsibilities.LossesCollector.APPLICATION)
                                .build())
                        .build())
                .setConfiguration(AccountCreateParams.Configuration.builder()
                        .setRecipient(AccountCreateParams.Configuration.Recipient.builder()
                                .setCapabilities(AccountCreateParams.Configuration.Recipient.Capabilities.builder()
                                        .setStripeBalance(AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.builder()
                                                .setStripeTransfers(AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.StripeTransfers.builder()
                                                        .setRequested(true)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            return client().v2().core().accounts().create(params, options("acct-" + userId)).getId();
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo crear la cuenta de cobros", e);
        }
    }

    /**
     * Genera el enlace de un solo uso a la página de alta de Stripe. Solo pide lo imprescindible
     * para empezar a cobrar ({@code currently_due}); si más adelante Stripe necesita algo más, lo
     * reclamará y el vendedor lo completará desde "Cobros con tarjeta".
     */
    public String createOnboardingLink(String accountId, String refreshUrl, String returnUrl) {
        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setUseCase(AccountLinkCreateParams.UseCase.builder()
                        .setType(AccountLinkCreateParams.UseCase.Type.ACCOUNT_ONBOARDING)
                        .setAccountOnboarding(AccountLinkCreateParams.UseCase.AccountOnboarding.builder()
                                .setRefreshUrl(refreshUrl)
                                .setReturnUrl(returnUrl)
                                .setCollectionOptions(AccountLinkCreateParams.UseCase.AccountOnboarding.CollectionOptions.builder()
                                        .setFields(AccountLinkCreateParams.UseCase.AccountOnboarding.CollectionOptions.Fields.CURRENTLY_DUE)
                                        .setFutureRequirements(AccountLinkCreateParams.UseCase.AccountOnboarding.CollectionOptions.FutureRequirements.OMIT)
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            return client().v2().core().accountLinks().create(params, options(null)).getUrl();
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo generar el enlace de alta", e);
        }
    }

    /** La cuenta (v2) ya puede recibir transferencias de la plataforma. */
    public boolean payoutsEnabled(String accountId) {
        try {
            com.stripe.model.v2.core.Account account = client().v2().core().accounts().retrieve(accountId,
                    AccountRetrieveParams.builder()
                            .addInclude(AccountRetrieveParams.Include.CONFIGURATION__RECIPIENT)
                            .build(),
                    options(null));
            var recipient = account.getConfiguration() != null ? account.getConfiguration().getRecipient() : null;
            var balance = recipient != null && recipient.getCapabilities() != null
                    ? recipient.getCapabilities().getStripeBalance() : null;
            var transfers = balance != null ? balance.getStripeTransfers() : null;
            return transfers != null && "active".equals(transfers.getStatus());
        } catch (StripeException e) {
            throw new PaymentProviderException("No se pudo consultar la cuenta de cobros", e);
        }
    }

    /** La cuenta puede recibir transferencias de la plataforma. */
    public boolean transfersActive(Account account) {
        return account.getCapabilities() != null
                && "active".equals(account.getCapabilities().getTransfers());
    }

    /**
     * Verifica la firma de una petición al webhook y devuelve el evento.
     *
     * @throws InvalidWebhookSignatureException si la firma no corresponde a ningún secreto configurado
     */
    public Event constructEvent(String payload, String signatureHeader) {
        if (signatureHeader == null || webhookSecrets.isEmpty()) {
            throw new InvalidWebhookSignatureException();
        }
        for (String secret : webhookSecrets) {
            try {
                return Webhook.constructEvent(payload, signatureHeader, secret);
            } catch (SignatureVerificationException e) {
                // probar con el siguiente secreto
            }
        }
        throw new InvalidWebhookSignatureException();
    }

    /** Objeto (PaymentIntent, Account...) que viaja dentro de un evento. */
    public StripeObject eventObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject().orElseGet(() -> {
            // La versión de API del evento no coincide con la del SDK: se deserializa igualmente,
            // los campos que usamos (id, status, latest_charge, capabilities) son estables.
            try {
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new PaymentProviderException("Evento de Stripe ilegible: " + event.getId(), e);
            }
        });
    }

    // Cliente para la API v2 (cuentas conectadas); la clave va igualmente en cada RequestOptions.
    private StripeClient client() {
        if (stripeClient == null) {
            stripeClient = new StripeClient(secretKey);
        }
        return stripeClient;
    }

    private RequestOptions options(String idempotencyKey) {
        if (!isConfigured()) {
            throw new PaymentProviderException("Los pagos con tarjeta no están disponibles en este momento");
        }
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(secretKey);
        if (idempotencyKey != null) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }
}
