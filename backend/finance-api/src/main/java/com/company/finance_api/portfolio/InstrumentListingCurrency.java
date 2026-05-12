package com.company.finance_api.portfolio;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.InstrumentType;

import java.util.Locale;
import java.util.Set;

/**
 * Listing / quote currency for an instrument's {@code price} and {@code totalAmount} on transactions.
 * Must stay aligned with trade execution and portfolio valuation paths.
 */
public final class InstrumentListingCurrency {

    /**
     * BIST names Yahoo maps as *.IS; prices are stored in TRY. If {@code exchange} drifts in DB,
     * these tickers still resolve as TRY so trade preview does not apply USD→TRY on native TL quotes.
     */
    private static final Set<String> KNOWN_TRY_LISTED_STOCKS = Set.of(
            "GARAN", "ASELS", "THYAO", "AKBNK", "YKBNK", "ISCTR", "EKGYO", "KCHOL", "TUPRS", "SAHOL"
    );

    private InstrumentListingCurrency() {
    }

    public static String resolve(Instrument instrument) {
        if (instrument.getExchange() != null && "BIST".equalsIgnoreCase(instrument.getExchange().name())) {
            return "TRY";
        }
        String symbol = instrument.getSymbol() == null ? "" : instrument.getSymbol().toUpperCase(Locale.ROOT);
        if (symbol.endsWith("TRY")) {
            return "TRY";
        }
        if (symbol.endsWith("EUR")) {
            return "EUR";
        }
        if (instrument.getType() == InstrumentType.STOCK && !symbol.isEmpty() && KNOWN_TRY_LISTED_STOCKS.contains(symbol)) {
            return "TRY";
        }
        return "USD";
    }
}
