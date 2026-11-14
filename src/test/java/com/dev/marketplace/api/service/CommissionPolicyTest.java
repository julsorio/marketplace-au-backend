package com.dev.marketplace.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CommissionPolicyTest {

    private final CommissionPolicy policy = new CommissionPolicy(new BigDecimal("2.5"), 50);

    @Test
    void sumaPorcentajeYFijo() {
        // 2.5 % de 120.00 = 3.00, más 0.50 fijo
        assertEquals(3.50, policy.feeFor(120.00));
    }

    @Test
    void redondeaAlCentimo() {
        // 2.5 % de 9.99 = 0.24975 -> 0.25, más 0.50
        assertEquals(0.75, policy.feeFor(9.99));
    }

    @Test
    void nuncaSuperaElImporte() {
        assertEquals(0.30, policy.feeFor(0.30));
    }
}
