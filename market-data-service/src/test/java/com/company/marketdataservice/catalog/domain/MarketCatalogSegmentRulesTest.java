package com.company.marketdataservice.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MarketCatalogSegmentRulesTest {

    @Test
    void pulseSegment_usEquityWithYahooSource_isNasdaqNotBist() {
        assertEquals("nasdaq", MarketCatalogSegmentRules.pulseSegment("AAPL", "STOCK", "YAHOO"));
    }

    @Test
    void pulseSegment_bistTickerWithYahoo_isBist() {
        assertEquals("bist", MarketCatalogSegmentRules.pulseSegment("AKBNK.IS", "STOCK", "YAHOO"));
    }

    @Test
    void pulseSegment_unknownStockWithoutSource_isNull() {
        assertNull(MarketCatalogSegmentRules.pulseSegment("FOO", "STOCK", null));
    }
}
