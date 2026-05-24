package com.company.marketdataservice.fund.infrastructure.kafka;
import com.company.marketdataservice.shared.kafka.MarketDataTopics;
import com.company.marketdataservice.fund.domain.FundSnapshotUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * `fon (TEFAS NAV)` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
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
