package com.company.marketdataservice.fund.infrastructure.kafka;
import com.company.marketdataservice.spot.infrastructure.snapshot.MarketSnapshotStore;
import com.company.marketdataservice.fund.domain.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.fund.infrastructure.kafka.FundSnapshotEventPublisher;
import com.company.marketdataservice.fund.infrastructure.kafka.KafkaFundSnapshotEventPublisher;
import com.company.marketdataservice.history.infrastructure.orchestration.BackfillExecutionContext;
import com.company.marketdataservice.history.infrastructure.write.FundHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * `fon (TEFAS NAV)` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RecordingFundSnapshotEventPublisher implements FundSnapshotEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final FundHistoryWriteService fundHistoryWriteService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final KafkaFundSnapshotEventPublisher delegate;

    @Override
    public void publishFundSnapshotUpdated(FundSnapshotUpdatedEvent event) {
        snapshotStore.recordFund(event);
        try {
            fundHistoryWriteService.save(event);
        } catch (Exception ex) {
            log.warn(
                    "fund_history_persist_failed eventId={} fundCode={} source={} reason={}",
                    event == null ? null : event.eventId(),
                    event == null ? null : event.fundCode(),
                    event == null ? null : event.source(),
                    ex.getMessage()
            );
        }
        if (BackfillExecutionContext.isActive() && !backfillProperties.getKafka().isEnabled()) {
            log.debug(
                    "BACKFILL_KAFKA_SKIPPED asset=FUND symbol={} eventId={}",
                    event == null ? null : event.fundCode(),
                    event == null ? null : event.eventId()
            );
            return;
        }
        delegate.publishFundSnapshotUpdated(event);
    }
}
