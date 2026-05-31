package com.company.marketdataservice.catalog.domain;

/**
 * Ingestion scope by market segment (maps to {@code instruments.exchange} / {@code instruments.type}).
 */
public enum IngestScopeSegment {
    BIST,
    NASDAQ,
    CRYPTO,
    /** All active {@code STOCK} rows (BIST + NASDAQ + other listings). */
    ALL_STOCKS
}
