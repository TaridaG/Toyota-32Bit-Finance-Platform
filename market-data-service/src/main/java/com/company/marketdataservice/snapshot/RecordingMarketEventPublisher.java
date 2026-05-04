package com.company.marketdataservice.snapshot;

import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.kafka.KafkaMarketEventPublisher;
import com.company.marketdataservice.kafka.MarketEventPublisher;
import com.company.marketdataservice.service.historical.BackfillExecutionContext;
import com.company.marketdataservice.service.history.MarketHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RecordingMarketEventPublisher implements MarketEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final KafkaMarketEventPublisher delegate;

    @Override
    public void publishMarketPriceUpdated(MarketPriceUpdatedEvent event) {
        snapshotStore.recordMarketPrice(event);
        try {
            marketHistoryWriteService.save(event);
        } catch (Exception ex) {
            log.warn(
                    "market_history_persist_failed eventId={} symbol={} source={} reason={}",
                    event == null ? null : event.eventId(),
                    event == null ? null : event.instrumentSymbol(),
                    event == null ? null : event.source(),
                    ex.getMessage()
            );
        }
        if (BackfillExecutionContext.isActive() && !backfillProperties.getKafka().isEnabled()) {
            log.debug(
                    "BACKFILL_KAFKA_SKIPPED asset=PRICE symbol={} eventId={}",
                    event == null ? null : event.instrumentSymbol(),
                    event == null ? null : event.eventId()
            );
            return;
        }
        delegate.publishMarketPriceUpdated(event);
    }
}
