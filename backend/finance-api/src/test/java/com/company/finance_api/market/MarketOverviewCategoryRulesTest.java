package com.company.finance_api.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketOverviewCategoryRulesTest {

    @Test
    void listingCurrency_spotMetalsAreTry() {
        assertEquals("TRY", MarketOverviewCategoryRules.listingCurrency("XAUTRY"));
        assertEquals("TRY", MarketOverviewCategoryRules.listingCurrency("XAGTRY"));
    }

    @Test
    void listingCurrency_usEquitiesAreUsd() {
        assertEquals("USD", MarketOverviewCategoryRules.listingCurrency("AAPL"));
        assertEquals("USD", MarketOverviewCategoryRules.listingCurrency("GC=F"));
    }

    @Test
    void listingCurrency_fxCrossesAreTry() {
        assertEquals("TRY", MarketOverviewCategoryRules.listingCurrency("USDTRY"));
    }
}
