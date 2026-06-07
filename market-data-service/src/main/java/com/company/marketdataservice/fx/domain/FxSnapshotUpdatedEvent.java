package com.company.marketdataservice.fx.domain;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * `FX spot` Kafka/domain event payload modeli.
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

    /**
     * Yeni bir {@code eventId} ve {@code occurredAt} atayarak FX snapshot güncelleme domain event'i oluşturur.
     *
     * @param canonicalSymbol platform canonical FX sembolü (ör. {@code USDTRY})
     * @param instrumentId finance catalog instrument ID; mapping yoksa {@code null}
     * @param baseCurrency baz para birimi kodu (ISO 4217)
     * @param quoteCurrency karşı para birimi kodu (ISO 4217)
     * @param bid alış (bid) fiyatı
     * @param ask satış (ask) fiyatı
     * @param mid orta (mid) fiyat
     * @param source veriyi sağlayan provider adı
     */
    public static FxSnapshotUpdatedEvent of(
            String canonicalSymbol,
            Long instrumentId,
            String baseCurrency,
            String quoteCurrency,
            BigDecimal bid,
            BigDecimal ask,
            BigDecimal mid,
            String source
    ) {
        return new FxSnapshotUpdatedEvent(
                UUID.randomUUID().toString(),
                canonicalSymbol,
                instrumentId,
                baseCurrency,
                quoteCurrency,
                bid,
                ask,
                mid,
                Instant.now(),
                source
        );
    }
}
