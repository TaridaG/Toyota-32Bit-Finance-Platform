package com.company.marketdataservice.fx.infrastructure.kafka;
import com.company.marketdataservice.fx.domain.FxSnapshotUpdatedEvent;

/**
 * `FX spot` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
public interface FxSnapshotEventPublisher {

    void publishFxSnapshotUpdated(FxSnapshotUpdatedEvent event);
}
