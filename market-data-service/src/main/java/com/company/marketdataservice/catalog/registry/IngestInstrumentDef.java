package com.company.marketdataservice.catalog.registry;

/**
 * Single ingest definition for platform registry sync and scheduler routing.
 */
public record IngestInstrumentDef(
        String symbol,
        String displayName,
        AssetKind kind,
        String exchange,
        QuoteCurrency quoteCurrency,
        IngestProvider provider,
        String providerSymbol,
        String tefasCode,
        String evdsSeries,
        String yahooChartSymbol
) {

    public IngestInstrumentDef {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName is required for " + symbol);
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind is required for " + symbol);
        }
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("exchange is required for " + symbol);
        }
        if (quoteCurrency == null) {
            throw new IllegalArgumentException("quoteCurrency is required for " + symbol);
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider is required for " + symbol);
        }
    }

    /** Resolved provider ticker for mapping rows (falls back to canonical symbol). */
    public String resolvedProviderSymbol() {
        if (providerSymbol != null && !providerSymbol.isBlank()) {
            return providerSymbol.trim();
        }
        return symbol.trim();
    }
}
