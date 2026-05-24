package com.company.finance_api.kafka;

/** MarketDataTopics — JPA domain entity (market data topics). */
public final class MarketDataTopics {
  private MarketDataTopics() {}

  public static final String MARKET_PRICE_UPDATED = "market.price.updated";
  public static final String MARKET_FX_SNAPSHOT_UPDATED = "market.fx.snapshot.updated";
  public static final String MARKET_FUND_SNAPSHOT_UPDATED = "market.fund.snapshot.updated";
}
