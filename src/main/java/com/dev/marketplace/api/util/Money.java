package com.dev.marketplace.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversión entre importes en unidades (como se guardan en el modelo, p. ej. 12.5 AUD) y céntimos
 * (como los espera Stripe). Pasa por BigDecimal para no arrastrar errores de redondeo de double.
 */
public final class Money {

    private Money() {
    }

    public static long toCents(double amount) {
        return BigDecimal.valueOf(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static double fromCents(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).doubleValue();
    }
}
