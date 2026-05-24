package com.company.marketdataservice.spot.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetalFuturesSpreadCalculatorTest {

    @Test
    void compute_returnsSpreadForValidInputs() {
        var result = MetalFuturesSpreadCalculator.compute(
                new BigDecimal("2000"),
                new BigDecimal("32.5"),
                new BigDecimal("65000")
        );
        assertNotNull(result);
        assertEquals(0, new BigDecimal("2000").compareTo(result.futuresUsd()));
        assertEquals(0, new BigDecimal("65000").compareTo(result.spotTryMid()));
        assertEquals(0, new BigDecimal("65000.0000").compareTo(result.impliedTryFromFutures()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(result.spreadAbsTry()));
        assertEquals(0, new BigDecimal("0.00").compareTo(result.spreadPct()));
    }

    @Test
    void compute_nullForInvalidInputs() {
        assertNull(MetalFuturesSpreadCalculator.compute(null, new BigDecimal("1"), new BigDecimal("1")));
        assertNull(MetalFuturesSpreadCalculator.compute(new BigDecimal("0"), new BigDecimal("1"), new BigDecimal("1")));
    }
}
