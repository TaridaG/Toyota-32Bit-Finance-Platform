package com.company.notification.insight.infrastructure.kafka;

import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.watchlist.domain.WatchlistProjection;
import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.insight.infrastructure.kafka.messaging.AnalyticsInsightMessage;
import com.company.notification.insight.infrastructure.persistence.PendingInsightEventRepository;
import com.company.notification.watchlist.application.SyncWatchlistProjectionUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@code analytics.insight.simple} event'lerini consume eder ve watchlist takipçileri için fiyat hareketi insight'larını kuyruğa alır.
 */
@Component
public class SimpleInsightEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SimpleInsightEventConsumer.class);
    private final SyncWatchlistProjectionUseCase watchlistProjectionService;
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final MeterRegistry meterRegistry;

    public SimpleInsightEventConsumer(
            SyncWatchlistProjectionUseCase watchlistProjectionService,
            PendingInsightEventRepository pendingInsightEventRepository,
            MeterRegistry meterRegistry
    ) {
        this.watchlistProjectionService = watchlistProjectionService;
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.meterRegistry = meterRegistry;
    }

    /** Instrument'ın her aktif watchlist takipçisi için pending insight satırları oluşturur. */
    @Transactional
    @KafkaListener(
            topics = KafkaTopicNames.ANALYTICS_INSIGHT_SIMPLE,
            groupId = "notification-service-insight-simple",
            containerFactory = "analyticsInsightKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, AnalyticsInsightMessage> record) {
        AnalyticsInsightMessage event = record.value();
        if (event == null || event.getInstrumentId() == null) {
            return;
        }
        meterRegistry.counter("notification_insight_received_total", "service", "notification-service").increment();
        List<WatchlistProjection> followers = watchlistProjectionService.findActiveFollowers(event.getInstrumentId());
        if (followers.isEmpty()) {
            return;
        }
        for (WatchlistProjection follower : followers) {
            PendingInsightEvent pending = new PendingInsightEvent();
            pending.setUserId(follower.getUserId());
            pending.setInstrumentId(event.getInstrumentId());
            pending.setSymbol(event.getSymbol() == null ? "" : event.getSymbol());
            pending.setChangePercent(event.getChangePercent());
            pending.setDirection(event.getDirection() == null ? "" : event.getDirection());
            pending.setOccurredAt(event.getOccurredAt());
            pending.setProcessed(false);
            pending.setEventType("INSIGHT");
            pending.setNewsTitle(null);
            try {
                pendingInsightEventRepository.save(pending);
            } catch (DataIntegrityViolationException ex) {
                meterRegistry.counter("notification_insight_duplicate_total", "service", "notification-service").increment();
                log.debug("INSIGHT_DUPLICATE_SKIPPED userId={} instrumentId={}", follower.getUserId(), event.getInstrumentId());
            }
        }
    }
}
