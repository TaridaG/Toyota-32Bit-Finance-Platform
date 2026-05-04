package com.company.marketdataservice.snapshot;

import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.kafka.FxSnapshotEventPublisher;
import com.company.marketdataservice.kafka.KafkaFxSnapshotEventPublisher;
import com.company.marketdataservice.service.historical.BackfillExecutionContext;
import com.company.marketdataservice.service.history.FxHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RecordingFxSnapshotEventPublisher implements FxSnapshotEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final FxHistoryWriteService fxHistoryWriteService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final KafkaFxSnapshotEventPublisher delegate;

    @Override
    public void publishFxSnapshotUpdated(FxSnapshotUpdatedEvent event) {
        snapshotStore.recordFx(event);
        try {
            fxHistoryWriteService.save(event);
        } catch (Exception ex) {
            log.warn(
                    "fx_history_persist_failed eventId={} symbol={} source={} reason={}",
                    event == null ? null : event.eventId(),
                    event == null ? null : event.canonicalSymbol(),
                    event == null ? null : event.source(),
                    ex.getMessage()
            );
        }
        if (BackfillExecutionContext.isActive() && !backfillProperties.getKafka().isEnabled()) {
            log.debug(
                    "BACKFILL_KAFKA_SKIPPED asset=FX symbol={} eventId={}",
                    event == null ? null : event.canonicalSymbol(),
                    event == null ? null : event.eventId()
            );
            return;
        }
        delegate.publishFxSnapshotUpdated(event);
    }
}
