package com.company.marketdataservice.fx.infrastructure.kafka;
import com.company.marketdataservice.shared.kafka.MarketDataTopics;
import com.company.marketdataservice.fx.domain.FxSnapshotUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * `FX spot` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
@Component
@RequiredArgsConstructor
public class KafkaFxSnapshotEventPublisher implements FxSnapshotEventPublisher {

    @SuppressWarnings("rawtypes")
    private final KafkaTemplate kafkaTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public void publishFxSnapshotUpdated(FxSnapshotUpdatedEvent event) {
        kafkaTemplate.send(MarketDataTopics.MARKET_FX_SNAPSHOT_UPDATED, event.canonicalSymbol(), event);
    }
}
