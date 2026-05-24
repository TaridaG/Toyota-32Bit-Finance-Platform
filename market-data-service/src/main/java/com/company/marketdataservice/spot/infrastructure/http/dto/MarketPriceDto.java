package com.company.marketdataservice.spot.infrastructure.http.dto;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `spot fiyat` REST API için HTTP DTO.
 */
public record MarketPriceDto(
        String symbol,
        BigDecimal price,
        String source,
        Instant timestamp,
        BigDecimal volume24h,
        BigDecimal openInterest,
        BigDecimal dayOpen,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        String exchangeName,
        String underlyingSymbol,
        Instant contractExpiry,
        String linkedSpotSymbol,
        BigDecimal spotSpreadPct,
        BigDecimal spotSpreadAbs) {

    public static MarketPriceDto basic(String symbol, BigDecimal price, String source, Instant timestamp) {
        return new MarketPriceDto(
                symbol,
                price,
                source,
                timestamp,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
