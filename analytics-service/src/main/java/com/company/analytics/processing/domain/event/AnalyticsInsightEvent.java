package com.company.analytics.processing.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Analytics pipeline tarafından üretilen insight event payload'ı.
 *
 * @param eventId       Event benzersiz kimliği
 * @param instrumentId  Instrument kimliği
 * @param symbol        Instrument sembolü
 * @param changePercent Yüzde fiyat değişimi
 * @param direction     Değişim yönü (ör. UP, DOWN)
 * @param occurredAt    Event oluşma zaman damgası
 */
public record AnalyticsInsightEvent(
        UUID eventId,
        Long instrumentId,
        String symbol,
        BigDecimal changePercent,
        String direction,
        Instant occurredAt
) {
}
