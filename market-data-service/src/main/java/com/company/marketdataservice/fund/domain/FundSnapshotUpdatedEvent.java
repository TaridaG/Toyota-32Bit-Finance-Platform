package com.company.marketdataservice.fund.domain;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * `fon (TEFAS NAV)` Kafka/domain event payload modeli.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FundSnapshotUpdatedEvent(
        String eventId,
        String fundCode,
        Long instrumentId,
        BigDecimal nav,
        Instant occurredAt,
        String source
) implements Serializable {

    /**
     * Yeni {@code eventId} ve {@code occurredAt} ile fon NAV güncelleme domain event'i oluşturur.
     *
     * @param fundCode fon kodu (TEFAS)
     * @param instrumentId finance catalog instrument ID; mapping yoksa {@code null}
     * @param nav güncel birim fiyat (NAV)
     * @param source veriyi sağlayan provider adı
     */
    public static FundSnapshotUpdatedEvent of(String fundCode, Long instrumentId, BigDecimal nav, String source) {
        return new FundSnapshotUpdatedEvent(
                UUID.randomUUID().toString(),
                fundCode,
                instrumentId,
                nav,
                Instant.now(),
                source
        );
    }
}
