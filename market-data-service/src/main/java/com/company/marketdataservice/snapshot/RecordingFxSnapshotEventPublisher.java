package com.company.marketdataservice.snapshot;

import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.kafka.FxSnapshotEventPublisher;
import com.company.marketdataservice.kafka.KafkaFxSnapshotEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class RecordingFxSnapshotEventPublisher implements FxSnapshotEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final KafkaFxSnapshotEventPublisher delegate;

    @Override
    public void publishFxSnapshotUpdated(FxSnapshotUpdatedEvent event) {
        snapshotStore.recordFx(event);
        delegate.publishFxSnapshotUpdated(event);
    }
}
