package com.company.marketdataservice.kafka;

import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;

public interface FundSnapshotEventPublisher {

    void publishFundSnapshotUpdated(FundSnapshotUpdatedEvent event);
}
