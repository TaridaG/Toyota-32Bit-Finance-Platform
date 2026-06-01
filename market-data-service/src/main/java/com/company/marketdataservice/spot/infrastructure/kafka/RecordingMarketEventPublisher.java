package com.company.marketdataservice.spot.infrastructure.kafka;
import com.company.marketdataservice.spot.application.SpotPricePublishValidator;
import com.company.marketdataservice.spot.infrastructure.snapshot.MarketSnapshotStore;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.spot.infrastructure.kafka.KafkaMarketEventPublisher;
import com.company.marketdataservice.spot.infrastructure.kafka.MarketEventPublisher;
import com.company.marketdataservice.history.infrastructure.orchestration.BackfillExecutionContext;
import com.company.marketdataservice.history.infrastructure.write.MarketHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * `spot fiyat` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RecordingMarketEventPublisher implements MarketEventPublisher {

    private final MarketSnapshotStore snapshotStore;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final KafkaMarketEventPublisher delegate;
    private final SpotPricePublishValidator spotPricePublishValidator;

    @Override
    public void publishMarketPriceUpdated(MarketPriceUpdatedEvent event) {
        if (!spotPricePublishValidator.shouldAccept(event)) {
            return;
        }
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
