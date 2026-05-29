package com.company.marketdataservice.catalog.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketCatalogSegmentRulesTest {

    @Test
    void inferWireCategory_cryptoUsdtSuffix() {
        assertEquals("CRYPTO", MarketCatalogSegmentRules.inferWireCategory("BTCUSDT"));
    }

    @Test
    void inferWireCategory_fxTrySuffix() {
        assertEquals("FX", MarketCatalogSegmentRules.inferWireCategory("USDTRY"));
    }

    @Test
    void inferWireCategory_metalSpot() {
        assertEquals("METAL", MarketCatalogSegmentRules.inferWireCategory("XAUTRY"));
    }

    @Test
    void inferWireCategory_bondPrefix() {
        assertEquals("BOND", MarketCatalogSegmentRules.inferWireCategory("TRBOND10Y"));
    }

    @Test
    void pulseSegment_yahooStockIsBist() {
        assertEquals("bist", MarketCatalogSegmentRules.pulseSegment("THYAO", "STOCK", "YAHOO"));
    }

    @Test
    void pulseSegment_finnhubStockIsNasdaq() {
        assertEquals("nasdaq", MarketCatalogSegmentRules.pulseSegment("AAPL", "STOCK", "FINNHUB"));
    }

    @Test
    void pulseSegment_unknownStockReturnsNull() {
        assertNull(MarketCatalogSegmentRules.pulseSegment("AAPL", "STOCK", "UNKNOWN"));
    }

    @Test
    void usesFxHistoryPath_forTryPair() {
        assertTrue(MarketCatalogSegmentRules.usesFxHistoryPath("EURTRY", "FX"));
    }

    @Test
    void usesFxHistoryPath_forSpotMetalEvenWhenCategoryStock() {
        assertTrue(MarketCatalogSegmentRules.usesFxHistoryPath("XAUTRY", "STOCK"));
    }

    @Test
    void usesFxHistoryPath_falseForEquity() {
        assertFalse(MarketCatalogSegmentRules.usesFxHistoryPath("AAPL", "STOCK"));
    }
}
