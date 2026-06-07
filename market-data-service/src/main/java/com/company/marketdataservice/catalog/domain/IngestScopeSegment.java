package com.company.marketdataservice.catalog.domain;

/**
 * Market segment'e göre ingestion scope; {@code instruments.exchange} / {@code instruments.type} ile eşlenir.
 */
public enum IngestScopeSegment {
    BIST,
    NASDAQ,
    CRYPTO,
    /** Aktif tüm {@code STOCK} satırları (BIST + NASDAQ + diğer listing'ler). */
    ALL_STOCKS
}
