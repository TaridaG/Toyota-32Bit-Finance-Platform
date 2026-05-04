package com.company.marketdataservice.snapshot;

import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.kafka.FundSnapshotEventPublisher;
import com.company.marketdataservice.kafka.KafkaFundSnapshotEventPublisher;
import com.company.marketdataservice.service.historical.BackfillExecutionContext;
import com.company.marketdataservice.service.history.FundHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
