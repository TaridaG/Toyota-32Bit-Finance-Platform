package com.company.marketdataservice.config;

import java.util.List;
import java.util.Map;

/**
 * Single source of truth for tracked crypto USDT pairs on Markets (Kripto) and MDS ingest.
 * Add symbols here, then run finance-api + market-data-service Flyway migrations for new rows.
 */
public final class TrackedCryptoSymbols {

    private TrackedCryptoSymbols() {}

    public static final List<String> SYMBOLS = List.of(
            "BTCUSDT",
            "ETHUSDT",
            "BNBUSDT",
            "SOLUSDT",
            "XRPUSDT",
            "DOGEUSDT",
            "ADAUSDT",
            "AVAXUSDT",
            "LINKUSDT",
            "TRXUSDT");

    /** Base asset (e.g. BTC) → CoinGecko coin id. */
    public static final Map<String, String> COINGECKO_ID_BY_BASE = Map.ofEntries(
            Map.entry("BTC", "bitcoin"),
            Map.entry("ETH", "ethereum"),
            Map.entry("BNB", "binancecoin"),
            Map.entry("SOL", "solana"),
            Map.entry("XRP", "ripple"),
            Map.entry("DOGE", "dogecoin"),
            Map.entry("ADA", "cardano"),
            Map.entry("AVAX", "avalanche-2"),
            Map.entry("LINK", "chainlink"),
            Map.entry("TRX", "tron"));
}
