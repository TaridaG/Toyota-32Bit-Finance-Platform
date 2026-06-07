package com.company.marketdataservice.catalog.registry;

/**
 * Canlı ve history provider tanımlayıcısı; değerler {@code mds_provider_instrument_mapping.provider} ile eşleşir.
 */
public enum IngestProvider {
    YAHOO,
    FINNHUB,
    BINANCE,
    COINGECKO,
    TEFAS,
    TCMB,
    TCMB_BOND,
    BIST_DERIVATIVES,
    COMPOSITE
}
