package com.company.marketdataservice.catalog.registry.providers;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;

import java.util.List;

/**
 * Yahoo Finance ({@code *.IS} ticker) üzerinden poll edilen BIST equity tanımlarını tutan ingest registry'sidir.
 */
public final class BistRegistry {

    private BistRegistry() {}

    private static final List<String> SYMBOLS = List.of(
            "GARAN", "THYAO", "ASELS", "AKBNK", "AVGYO", "EREGL", "BIMAS",
            "TUPRS", "ISCTR", "SAHOL", "AVPGY", "SARKY");

    public static List<IngestInstrumentDef> all() {
        return SYMBOLS.stream().map(BistRegistry::def).toList();
    }

    private static IngestInstrumentDef def(String symbol) {
        return new IngestInstrumentDef(
                symbol,
                symbol + " BIST",
                AssetKind.STOCK,
                "BIST",
                QuoteCurrency.TRY,
                IngestProvider.YAHOO,
                symbol + ".IS",
                null,
                null,
                null
        );
    }
}
