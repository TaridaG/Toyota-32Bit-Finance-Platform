package com.company.marketdataservice.kafka;

import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;

public interface FxSnapshotEventPublisher {

    void publishFxSnapshotUpdated(FxSnapshotUpdatedEvent event);
}
