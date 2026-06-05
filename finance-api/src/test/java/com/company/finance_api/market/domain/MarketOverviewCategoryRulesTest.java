package com.company.finance_api.market.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarketOverviewCategoryRulesTest {

  @Test
  void matchesUiCategory_aaplYahoo_onNasdaqTab_matches() {
    assertTrue(MarketOverviewCategoryRules.matchesUiCategory("AAPL", "YAHOO", null, "NASDAQ"));
  }

  @Test
  void matchesUiCategory_aaplYahoo_onBistTab_doesNotMatch() {
    assertFalse(MarketOverviewCategoryRules.matchesUiCategory("AAPL", "YAHOO", null, "BIST"));
  }

  @Test
  void matchesUiCategory_akbnkYahoo_onBistTab_matches() {
    assertTrue(MarketOverviewCategoryRules.matchesUiCategory("AKBNK", "YAHOO", "BIST", "BIST"));
  }

  @Test
  void matchesUiCategory_crmYahoo_onNasdaqTab_matchesViaExchange() {
    assertTrue(MarketOverviewCategoryRules.matchesUiCategory("CRM", "YAHOO", "NASDAQ", "NASDAQ"));
  }

  @Test
  void matchesUiCategory_vooFinnhub_onNasdaqTab_doesNotMatch() {
    assertFalse(MarketOverviewCategoryRules.matchesUiCategory("VOO", "FINNHUB", "FINNHUB", "NASDAQ"));
  }

  @Test
  void matchesUiCategory_viopBist_onBistTab_doesNotMatch() {
    assertFalse(
        MarketOverviewCategoryRules.matchesUiCategory("VIOP_DIBS_NEAR", "YAHOO", "BIST", "BIST"));
  }

  @Test
  void matchesUiCategory_trGovUsd_onBistTab_doesNotMatch() {
    assertFalse(
        MarketOverviewCategoryRules.matchesUiCategory("TRGOVUSD15Y", "TCMB", "BIST", "BIST"));
  }
}
