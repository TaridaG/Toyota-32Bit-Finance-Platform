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
}
