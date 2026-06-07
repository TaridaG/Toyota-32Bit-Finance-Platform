package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.List;

/**
 * Fiat TRY cross'ları ve değerli metal spot FX enstrümanları; TCMB mapping'lerine sync edilir.
 */
public final class FxRegistry {

    private FxRegistry() {}

    public static List<IngestInstrumentDef> all() {
        return List.of(
                fx("USDTRY", "USD/TRY", "USD"),
                fx("EURTRY", "EUR/TRY", "EUR"),
                fx("GBPTRY", "GBP/TRY", "GBP"),
                fx("JPYTRY", "JPY/TRY", "JPY"),
                fx("AEDTRY", "AED/TRY", "AED"),
                fx("XAUTRY", "Gold TRY", "XAU"),
                fx("XAGTRY", "Silver TRY", "XAG"),
                fx("XPTTRY", "Platinum TRY", "XPT"),
                fx("XPDTRY", "Palladium TRY", "XPD"),
                fx("XCUTRY", "Copper TRY", "XCU")
        );
    }

    private static IngestInstrumentDef fx(String symbol, String displayName, String baseCurrency) {
        QuoteCurrency quote = QuoteCurrency.TRY;
        return new IngestInstrumentDef(
                symbol,
                displayName,
                AssetKind.FX,
                "TCMB",
                quote,
                IngestProvider.TCMB,
                symbol,
                null,
                null,
                null
        );
    }

    /** Catalog {@code base_currency} kolonu için base currency kodunu döner. */
    public static String baseCurrencyFor(String symbol) {
        if (symbol == null || symbol.length() < 6 || !symbol.endsWith("TRY")) {
            return null;
        }
        return symbol.substring(0, symbol.length() - 3);
    }
}
