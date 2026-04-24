package com.company.marketdataservice.kafka;

import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaFundSnapshotEventPublisher implements FundSnapshotEventPublisher {

    @SuppressWarnings("rawtypes")
    private final KafkaTemplate kafkaTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public void publishFundSnapshotUpdated(FundSnapshotUpdatedEvent event) {
        kafkaTemplate.send(MarketDataTopics.MARKET_FUND_SNAPSHOT_UPDATED, event.fundCode(), event);
    }
}
