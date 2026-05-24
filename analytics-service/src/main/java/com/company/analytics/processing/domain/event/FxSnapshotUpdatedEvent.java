package com.company.analytics.processing.domain.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kafka {@code market.fx.snapshot.updated} topic'inden gelen FX snapshot event payload'ı.
 *
 * @param eventId          Event benzersiz kimliği
 * @param canonicalSymbol  Kanonik instrument sembolü
 * @param instrumentId     Instrument kimliği
 * @param baseCurrency     Baz para birimi
 * @param quoteCurrency    Karşı para birimi
 * @param bid              Bid fiyatı
 * @param ask              Ask fiyatı
 * @param mid              Mid (orta) fiyatı
 * @param occurredAt       Event oluşma zaman damgası
 * @param source           Veri kaynağı tanımlayıcısı
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FxSnapshotUpdatedEvent(
        String eventId,
        String canonicalSymbol,
        Long instrumentId,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        Instant occurredAt,
        String source
) implements Serializable {
}
