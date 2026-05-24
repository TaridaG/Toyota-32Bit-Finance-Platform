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
     * İş mantığı operasyonunu çalıştırır.
         * @param canonicalSymbol girdi parametresi
         * @param instrumentId girdi parametresi
         * @param baseCurrency girdi parametresi
         * @param quoteCurrency girdi parametresi
         * @param bid girdi parametresi
         * @param ask girdi parametresi
         * @param mid girdi parametresi
         * @param source provider adı
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
