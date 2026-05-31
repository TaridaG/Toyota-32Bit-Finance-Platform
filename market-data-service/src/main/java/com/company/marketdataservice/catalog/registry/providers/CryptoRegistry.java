package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Crypto USDT pairs; live poll uses composite provider, DB sync writes BINANCE + COINGECKO mappings.
 */
public final class CryptoRegistry {

    private CryptoRegistry() {}

    public static final List<String> SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
            "DOGEUSDT", "ADAUSDT", "AVAXUSDT", "LINKUSDT", "TRXUSDT");

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

    public static List<IngestInstrumentDef> all() {
        return SYMBOLS.stream().map(CryptoRegistry::def).toList();
    }

    public static String coingeckoIdForBase(String baseAsset) {
        if (baseAsset == null || baseAsset.isBlank()) {
            return null;
        }
        return COINGECKO_ID_BY_BASE.get(baseAsset.trim().toUpperCase(Locale.ROOT));
    }

    private static IngestInstrumentDef def(String symbol) {
        return new IngestInstrumentDef(
                symbol,
                symbol + " Crypto",
                AssetKind.CRYPTO,
                "BINANCE",
                QuoteCurrency.USDT,
                IngestProvider.COMPOSITE,
                symbol,
                null,
                null,
                null
        );
    }
}
