package com.company.marketdataservice.fund.infrastructure.kafka;
import com.company.marketdataservice.fund.domain.FundSnapshotUpdatedEvent;

/**
 * `fon (TEFAS NAV)` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
public interface FundSnapshotEventPublisher {

    void publishFundSnapshotUpdated(FundSnapshotUpdatedEvent event);
}
