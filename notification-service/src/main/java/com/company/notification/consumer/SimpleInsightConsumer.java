package com.company.notification.consumer;

import com.company.notification.domain.PendingInsightEvent;
import com.company.notification.domain.WatchlistProjection;
import com.company.notification.event.AnalyticsInsightEvent;
import com.company.notification.repository.PendingInsightEventRepository;
import com.company.notification.service.WatchlistProjectionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SimpleInsightConsumer {

    private static final Logger log = LoggerFactory.getLogger(SimpleInsightConsumer.class);
    private final WatchlistProjectionService watchlistProjectionService;
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final MeterRegistry meterRegistry;

    public SimpleInsightConsumer(
            WatchlistProjectionService watchlistProjectionService,
            PendingInsightEventRepository pendingInsightEventRepository,
            MeterRegistry meterRegistry
    ) {
        this.watchlistProjectionService = watchlistProjectionService;
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    @KafkaListener(
            topics = "analytics.insight.simple",
            groupId = "notification-service-insight-simple",
            containerFactory = "analyticsInsightKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, AnalyticsInsightEvent> record) {
        AnalyticsInsightEvent event = record.value();
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
