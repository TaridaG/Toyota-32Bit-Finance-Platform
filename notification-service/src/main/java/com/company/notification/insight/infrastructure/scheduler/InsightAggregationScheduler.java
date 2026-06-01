package com.company.notification.insight.infrastructure.scheduler;

import com.company.notification.insight.application.DeliverWatchlistDigestUseCase;
import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.persistence.PendingInsightEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pending insight/haber satırlarını periyodik olarak kullanıcı bazında gruplar,
 * digest e-posta ve portal kutusuna teslim eder, ardından processed işaretler.
 */
@Component
public class InsightAggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightAggregationScheduler.class);
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final DeliverWatchlistDigestUseCase deliverWatchlistDigestUseCase;
    private final MeterRegistry meterRegistry;

    public InsightAggregationScheduler(
            PendingInsightEventRepository pendingInsightEventRepository,
            DeliverWatchlistDigestUseCase deliverWatchlistDigestUseCase,
            MeterRegistry meterRegistry
    ) {
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.deliverWatchlistDigestUseCase = deliverWatchlistDigestUseCase;
        this.meterRegistry = meterRegistry;
    }

    /**
     * İşlenmemiş event'leri yükler, kullanıcı bazında digest teslim eder ve processed işaretler.
     */
    @Transactional
    @Scheduled(fixedDelayString = "${notification.insight.aggregation-interval-ms:21600000}")
    public void aggregateAndSend() {
        List<PendingInsightEvent> pendingEvents = pendingInsightEventRepository.findByProcessedFalse();
        if (pendingEvents.isEmpty()) {
            return;
        }

        Map<UUID, List<PendingInsightEvent>> groupedByUser = pendingEvents.stream()
                .collect(Collectors.groupingBy(PendingInsightEvent::getUserId));

        meterRegistry.counter("notification_insight_aggregated_total", "service", "notification-service")
                .increment(groupedByUser.size());

        for (Map.Entry<UUID, List<PendingInsightEvent>> entry : groupedByUser.entrySet()) {
            try {
                deliverDigest(entry.getKey(), entry.getValue());
            } catch (Exception ex) {
                log.error("INSIGHT_AGGREGATION_FAILED userId={} pendingCount={}", entry.getKey(), entry.getValue().size(), ex);
            }
        }
    }

    private void deliverDigest(UUID userId, List<PendingInsightEvent> userEvents) {
        deliverWatchlistDigestUseCase.deliver(userId, userEvents);
        meterRegistry.counter("notification_insight_sent_total", "service", "notification-service").increment();
        for (PendingInsightEvent event : userEvents) {
            event.setProcessed(true);
        }
        pendingInsightEventRepository.saveAll(userEvents);
    }
}
