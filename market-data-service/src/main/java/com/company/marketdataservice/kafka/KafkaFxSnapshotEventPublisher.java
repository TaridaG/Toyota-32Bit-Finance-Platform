package com.company.marketdataservice.kafka;

import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
