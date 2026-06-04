package com.company.marketdataservice.fx.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FxQuoteNormalizationTest {

    @Test
    void jpyTry_tcmbPer100_scaledToTryPerOneJpy() {
        BigDecimal n = FxQuoteNormalization.normalizePrice("JPYTRY", new BigDecimal("28.7"));
        assertEquals(0, new BigDecimal("0.287").compareTo(n));
    }

    @Test
    void jpyTry_alreadyPerOneJpy_unchanged() {
        BigDecimal n = FxQuoteNormalization.normalizePrice("JPYTRY", new BigDecimal("0.287"));
        assertEquals(0, new BigDecimal("0.287").compareTo(n));
    }

    @Test
    void usdTry_unchanged() {
        BigDecimal n = FxQuoteNormalization.normalizePrice("USDTRY", new BigDecimal("32.50"));
        assertEquals(0, new BigDecimal("32.50").compareTo(n));
    }
}
