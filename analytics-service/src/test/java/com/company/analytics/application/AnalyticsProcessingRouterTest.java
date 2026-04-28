package com.company.analytics.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsProcessingRouterTest {

    private final AnalyticsProcessingRouter router = new AnalyticsProcessingRouter();

    @Test
    void market_should_enable_all_processors() {
        AnalyticsProcessingDecision decision = router.decide("MARKET");

        assertEquals(true, decision.processCandle());
        assertEquals(true, decision.processMovingAverage());
        assertEquals(true, decision.processRsi());
        assertEquals(true, decision.processTrend());
    }

    @Test
    void fxMid_should_enable_candle_and_ma_only() {
        AnalyticsProcessingDecision decision = router.decide("FX_MID");

        assertEquals(true, decision.processCandle());
        assertEquals(true, decision.processMovingAverage());
        assertEquals(false, decision.processRsi());
        assertEquals(false, decision.processTrend());
    }

    @Test
    void fundNav_should_enable_only_candle() {
        AnalyticsProcessingDecision decision = router.decide("FUND_NAV");

        assertEquals(true, decision.processCandle());
        assertEquals(false, decision.processMovingAverage());
        assertEquals(false, decision.processRsi());
        assertEquals(false, decision.processTrend());
    }

    @Test
    void unknown_should_enable_only_candle() {
        AnalyticsProcessingDecision decision = router.decide("SOMETHING_ELSE");

        assertEquals(true, decision.processCandle());
        assertEquals(false, decision.processMovingAverage());
        assertEquals(false, decision.processRsi());
        assertEquals(false, decision.processTrend());
    }
}
