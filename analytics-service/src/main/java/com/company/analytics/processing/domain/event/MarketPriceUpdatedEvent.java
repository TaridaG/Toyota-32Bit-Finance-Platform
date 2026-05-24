package com.company.analytics.processing.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kafka {@code market.price.updated} topic'inden gelen market price event payload'ı.
 *
 * @param eventId          Event benzersiz kimliği
 * @param instrumentSymbol Instrument sembolü
 * @param price            Fiyat değeri
 * @param priceType        Fiyat tipi (ör. MARKET, LAST)
 * @param source           Veri kaynağı tanımlayıcısı
 * @param occurredAt       Event oluşma zaman damgası
 * @param instrumentId     Instrument kimliği (opsiyonel; legacy event'lerde eksik olabilir)
 */
public record MarketPriceUpdatedEvent(
        String eventId,
        String instrumentSymbol,
        BigDecimal price,
        String priceType,
        String source,
        Instant occurredAt,
        Long instrumentId
) {
}
