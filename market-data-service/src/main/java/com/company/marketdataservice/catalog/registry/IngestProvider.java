package com.company.marketdataservice.catalog.registry;

/**
 * Live/history provider identifier; values match {@code mds_provider_instrument_mapping.provider}.
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
