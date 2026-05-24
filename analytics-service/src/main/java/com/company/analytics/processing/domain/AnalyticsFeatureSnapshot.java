package com.company.analytics.processing.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Belirli bir zaman noktasında instrument için hesaplanmış feature snapshot değerlerini taşır.
 *
 * @param instrumentId   Instrument kimliği
 * @param timestamp      Snapshot zaman damgası
 * @param price          Güncel fiyat
 * @param volatility     Volatilite metriği
 * @param momentum       Momentum metriği
 * @param priceChange1h  Son 1 saatlik fiyat değişimi
 * @param priceChange24h Son 24 saatlik fiyat değişimi
 * @param source         Veri kaynağı tanımlayıcısı
 */
public record AnalyticsFeatureSnapshot(
        Long instrumentId,
        Instant timestamp,
        BigDecimal price,
        BigDecimal volatility,
        BigDecimal momentum,
        BigDecimal priceChange1h,
        BigDecimal priceChange24h,
        String source
) {
}
