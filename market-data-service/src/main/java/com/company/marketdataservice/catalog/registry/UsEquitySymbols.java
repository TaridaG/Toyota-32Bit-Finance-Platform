package com.company.marketdataservice.catalog.registry;

import java.util.Locale;
import java.util.Set;

/**
 * US equities / ETFs in the platform ingest registry ({@link providers.NasdaqRegistry}).
 * Used for market segment classification when live quotes fall back to Yahoo ({@code source=YAHOO}).
 */
public final class UsEquitySymbols {

    private static final Set<String> SYMBOLS =
            Set.of(
                    "AAPL", "AMZN", "NVDA", "MSFT", "GOOGL", "TSLA", "META", "AVGO",
                    "AMD", "NFLX", "INTC", "CSCO",
                    "VOO", "VTI", "QQQ", "IVV", "SPY");

    private UsEquitySymbols() {}

    public static boolean isUsListedEquity(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        return SYMBOLS.contains(symbol.trim().toUpperCase(Locale.ROOT));
    }
}
