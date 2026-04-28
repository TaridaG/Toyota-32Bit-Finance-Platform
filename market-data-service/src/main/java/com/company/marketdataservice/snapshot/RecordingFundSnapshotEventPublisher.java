package com.company.marketdataservice.snapshot;

import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.kafka.FundSnapshotEventPublisher;
import com.company.marketdataservice.kafka.KafkaFundSnapshotEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class RecordingFundSnapshotEventPublisher implements FundSnapshotEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final KafkaFundSnapshotEventPublisher delegate;

    @Override
    public void publishFundSnapshotUpdated(FundSnapshotUpdatedEvent event) {
        snapshotStore.recordFund(event);
        delegate.publishFundSnapshotUpdated(event);
    }
}
