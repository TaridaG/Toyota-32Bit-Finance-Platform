package com.company.analytics.processing.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Analytics processing pipeline'ında kullanılan normalize edilmiş market price event'i.
 *
 * @param eventId          Event benzersiz kimliği
 * @param instrumentId     Instrument kimliği
 * @param instrumentSymbol Instrument sembolü
 * @param price            Fiyat değeri
 * @param occurredAt       Event oluşma zaman damgası
 * @param priceType        Fiyat tipi (ör. MARKET, FX_MID)
 */
public record AnalyticsMarketPriceEvent(
        String eventId,
        Long instrumentId,
        String instrumentSymbol,
        BigDecimal price,
        Instant occurredAt,
        String priceType
) {
    /**
     * Varsayılan {@code priceType} değeri {@code MARKET} olan compact constructor.
     *
     * @param eventId          Event benzersiz kimliği
     * @param instrumentId     Instrument kimliği
     * @param instrumentSymbol Instrument sembolü
     * @param price            Fiyat değeri
     * @param occurredAt       Event oluşma zaman damgası
     */
    public AnalyticsMarketPriceEvent(
            String eventId,
            Long instrumentId,
            String instrumentSymbol,
            BigDecimal price,
            Instant occurredAt
    ) {
        this(eventId, instrumentId, instrumentSymbol, price, occurredAt, "MARKET");
    }
}
