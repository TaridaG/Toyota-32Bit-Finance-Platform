package com.company.marketdataservice.shared.kafka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketDataTopicsTest {

    @Test
    void exposesStableTopicNames() {
        assertEquals("market.price.updated", MarketDataTopics.MARKET_PRICE_UPDATED);
        assertEquals("market.fx.snapshot.updated", MarketDataTopics.MARKET_FX_SNAPSHOT_UPDATED);
        assertEquals("market.fund.snapshot.updated", MarketDataTopics.MARKET_FUND_SNAPSHOT_UPDATED);
    }
}
