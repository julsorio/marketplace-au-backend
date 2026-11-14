package com.dev.marketplace.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dev.marketplace.api.util.Money;

/**
 * Comisión que la plataforma descuenta al vendedor en cada venta con tarjeta: un porcentaje más un
 * fijo, con la misma forma que la tarifa de Stripe para cubrirla (incluida la parte que Stripe no
 * devuelve en los reembolsos). Los valores vienen de {@code marketplace.commission.*}.
 */
@Component
public class CommissionPolicy {
    private final BigDecimal percent;
    private final long fixedCents;

    public CommissionPolicy(@Value("${marketplace.commission.percent}") BigDecimal percent,
            @Value("${marketplace.commission.fixed-cents}") long fixedCents) {
        this.percent = percent;
        this.fixedCents = fixedCents;
    }

    /**
     * Calcula la comisión para un importe. Nunca supera el propio importe.
     *
     * @param amount importe cobrado al comprador, en unidades (p. ej. 120.0 AUD)
     * @return comisión en unidades
     */
    public double feeFor(double amount) {
        long amountCents = Money.toCents(amount);
        long feeCents = BigDecimal.valueOf(amountCents).multiply(percent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact() + fixedCents;
        return Money.fromCents(Math.min(feeCents, amountCents));
    }
}
