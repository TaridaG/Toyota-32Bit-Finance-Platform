package com.company.finance_api.market.infrastructure.http.dto;

import java.math.BigDecimal;

/** MarketOverviewItemResponse — API transfer nesnesi (DTO/response/request). */
public record MarketOverviewItemResponse(
    String symbol,
    String name,
    /**
     * Raw last price in the instrument's listing/feed currency (before {@code X-Currency}
     * conversion).
     */
    BigDecimal nativePrice,
    /** Price converted into the requested {@code X-Currency} header. */
    BigDecimal price,
    BigDecimal change24h,
    BigDecimal change1D,
    BigDecimal change1M,
    BigDecimal change3M,
    BigDecimal change6M,
    BigDecimal change1Y,
    BigDecimal high24h,
    BigDecimal low24h,
    String category,
    Long instrumentId,
    /** 0–100 style score derived from analytics trend momentum (nullable when unavailable). */
    BigDecimal trendScore,
    /** UI label: WEAK, NEUTRAL, STRONG, VERY_STRONG (nullable). */
    String trendLabel,
    BigDecimal volume24h,
    BigDecimal openInterest,
    BigDecimal dayOpen,
    BigDecimal dayHigh,
    BigDecimal dayLow,
    String exchangeName,
    String underlyingSymbol,
    java.time.Instant contractExpiry,
    String linkedSpotSymbol,
    BigDecimal spotSpreadPct,
    BigDecimal spotSpreadAbs) {}
