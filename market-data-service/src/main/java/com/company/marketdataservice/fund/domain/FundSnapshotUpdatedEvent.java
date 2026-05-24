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
     * İş mantığı operasyonunu çalıştırır.
         * @param fundCode girdi parametresi
         * @param instrumentId girdi parametresi
         * @param nav girdi parametresi
         * @param source provider adı
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
