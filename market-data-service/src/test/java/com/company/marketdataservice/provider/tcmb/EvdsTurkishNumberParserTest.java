package com.company.marketdataservice.provider.tcmb;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EvdsTurkishNumberParserTest {

    @Test
    void commaDecimal() {
        assertEquals(new BigDecimal("37.50"), EvdsTurkishNumberParser.parsePercentLike("37,50"));
    }

    @Test
    void thousandsDotCommaFraction() {
        assertEquals(new BigDecimal("3683.83"), EvdsTurkishNumberParser.parsePercentLike("3.683,83"));
    }

    @Test
    void plainEnglishDot() {
        assertEquals(new BigDecimal("37.5"), EvdsTurkishNumberParser.parsePercentLike("37.5"));
    }

    @Test
    void blank() {
        assertNull(EvdsTurkishNumberParser.parsePercentLike(""));
        assertNull(EvdsTurkishNumberParser.parsePercentLike("  "));
    }
}
