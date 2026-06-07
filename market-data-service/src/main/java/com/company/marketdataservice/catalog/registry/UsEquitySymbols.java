package com.company.marketdataservice.catalog.registry;

import java.util.Locale;
import java.util.Set;

/**
 * Platform ingest registry'deki US equity ve ETF sembolleri ({@link providers.NasdaqRegistry}).
 * Canlı quote Yahoo'ya fallback ettiğinde ({@code source=YAHOO}) market segment sınıflandırması için kullanılır.
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
