package com.company.finance_api.domain.enums;

public enum Exchange {
    BINANCE,
    BIST,
    /**
     * Legacy / seeded rows for Yahoo Finance–sourced BIST-style listings (DB may store {@code YAHOO}).
     * Kept so Hibernate can load existing {@code instruments.exchange} values.
     */
    YAHOO,
    /** Turkish mutual / investment funds (DB seed: {@code V32}, {@code V33}). */
    TEFAS,
    TCMB,
    NASDAQ,
    FINNHUB
}
