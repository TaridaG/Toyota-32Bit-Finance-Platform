package com.company.marketdataservice.spot.application;

import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpotPricePublishValidatorTest {

    private final SpotPricePublishValidator validator = new SpotPricePublishValidator();

    @Test
    void acceptsFirstTick() {
        var event = MarketPriceUpdatedEvent.of("AKBNK", new BigDecimal("64"), "MARKET", "YAHOO", 54L);
        assertTrue(validator.shouldAccept(event));
    }

    @Test
    void rejectsLargeJumpFromPrevious() {
        var first = MarketPriceUpdatedEvent.of("AKBNK", new BigDecimal("64"), "MARKET", "YAHOO", 54L);
        assertTrue(validator.shouldAccept(first));

        var spike = MarketPriceUpdatedEvent.ofAt(
                "AKBNK",
                new BigDecimal("114"),
                "MARKET",
                "YAHOO",
                54L,
                Instant.now()
        );
        assertFalse(validator.shouldAccept(spike));
    }
}
