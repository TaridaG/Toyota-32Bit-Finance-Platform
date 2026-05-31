package com.company.finance_api.market.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MarketOverviewCategoryRulesTest {

  @Test
  void listingCurrency_spotMetalsAreTry() {
    assertEquals("TRY", MarketOverviewCategoryRules.listingCurrency("XAUTRY"));
    assertEquals("TRY", MarketOverviewCategoryRules.listingCurrency("XAGTRY"));
  }

  @Test
  void listingCurrency_usEquitiesAreUsd() {
    assertEquals("USD", MarketOverviewCategoryRules.listingCurrency("AAPL"));
  }

  @Test
  void listingCurrency_fxCrossesAreTry() {
    assertEquals("TRY", MarketOverviewCategoryRules.listingCurrency("USDTRY"));
  }
}
