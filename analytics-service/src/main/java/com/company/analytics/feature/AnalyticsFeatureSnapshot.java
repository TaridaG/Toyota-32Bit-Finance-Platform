package com.company.analytics.feature;

import java.math.BigDecimal;
import java.time.Instant;

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
