package com.company.marketdataservice.snapshot;

import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.kafka.KafkaMarketEventPublisher;
import com.company.marketdataservice.kafka.MarketEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class RecordingMarketEventPublisher implements MarketEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final KafkaMarketEventPublisher delegate;

    @Override
    public void publishMarketPriceUpdated(MarketPriceUpdatedEvent event) {
        snapshotStore.recordMarketPrice(event);
        delegate.publishMarketPriceUpdated(event);
    }
}
