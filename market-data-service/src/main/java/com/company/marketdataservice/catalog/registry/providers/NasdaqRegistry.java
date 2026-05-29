package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * US equities and ETF funds polled via Finnhub when enabled.
 */
public final class NasdaqRegistry {

    private NasdaqRegistry() {}

    private static final Set<String> ETF_SYMBOLS = Set.of("VOO", "VTI", "QQQ", "IVV", "SPY");

    private static final List<String> STOCK_SYMBOLS = List.of(
            "AAPL", "AMZN", "NVDA", "MSFT", "GOOGL", "TSLA", "META", "AVGO",
            "AMD", "NFLX", "INTC", "CSCO");

    public static List<IngestInstrumentDef> all() {
        List<IngestInstrumentDef> out = new ArrayList<>(STOCK_SYMBOLS.size() + ETF_SYMBOLS.size());
        for (String symbol : STOCK_SYMBOLS) {
            out.add(stock(symbol));
        }
        for (String symbol : ETF_SYMBOLS) {
            out.add(etf(symbol));
        }
        return List.copyOf(out);
    }

    public static boolean isFinnhubOwned(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return STOCK_SYMBOLS.contains(normalized) || ETF_SYMBOLS.contains(normalized);
    }

    private static IngestInstrumentDef stock(String symbol) {
        return new IngestInstrumentDef(
                symbol,
                symbol + " NASDAQ",
                AssetKind.STOCK,
                "NASDAQ",
                QuoteCurrency.USD,
                IngestProvider.FINNHUB,
                symbol,
                null,
                null,
                null
        );
    }

    private static IngestInstrumentDef etf(String symbol) {
        return new IngestInstrumentDef(
                symbol,
                symbol + " ETF",
                AssetKind.FUND,
                "FINNHUB",
                QuoteCurrency.USD,
                IngestProvider.FINNHUB,
                symbol,
                null,
                null,
                null
        );
    }
}
