package com.company.notification.insight.infrastructure.scheduler;

import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.persistence.PendingInsightEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pending insight/haber satırlarını periyodik olarak kullanıcı bazında gruplar ve processed olarak işaretler.
 * <p>
 * Teslimat (e-posta veya portal) henüz implement edilmedi; job şu an digest özetini loglar.
 */
@Component
public class InsightAggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightAggregationScheduler.class);
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final MeterRegistry meterRegistry;

    public InsightAggregationScheduler(PendingInsightEventRepository pendingInsightEventRepository, MeterRegistry meterRegistry) {
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * İşlenmemiş event'leri yükler, kullanıcı bazında aggregate eder, top symbol'leri loglar ve hepsini processed işaretler.
     */
    @Transactional
    @Scheduled(fixedDelayString = "${notification.insight.aggregation-interval-ms:21600000}")
    public void aggregateAndSend() {
        List<PendingInsightEvent> pendingEvents = pendingInsightEventRepository.findByProcessedFalse();
        if (pendingEvents.isEmpty()) {
            return;
        }

        Map<java.util.UUID, List<PendingInsightEvent>> groupedByUser = pendingEvents.stream()
                .collect(Collectors.groupingBy(PendingInsightEvent::getUserId));

        meterRegistry.counter("notification_insight_aggregated_total", "service", "notification-service")
                .increment(groupedByUser.size());

        for (Map.Entry<java.util.UUID, List<PendingInsightEvent>> entry : groupedByUser.entrySet()) {
            try {
                deliverDigest(entry.getKey(), entry.getValue());
            } catch (Exception ex) {
                log.error("INSIGHT_AGGREGATION_FAILED userId={} pendingCount={}", entry.getKey(), entry.getValue().size(), ex);
            }
        }
    }

    private void deliverDigest(java.util.UUID userId, List<PendingInsightEvent> userEvents) {
            List<PendingInsightEvent> topEvents = userEvents.stream()
                    .sorted(Comparator.comparing(
                            e -> e.getChangePercent() == null ? BigDecimal.ZERO : e.getChangePercent().abs(),
                            Comparator.reverseOrder()
                    ))
                    .limit(5)
                    .toList();

            String symbols = topEvents.stream()
                    .map(e -> e.getSymbol() + " " + formatChangeOrNews(e))
                    .collect(Collectors.joining(","));

            log.info("INSIGHT_NOTIFICATION userId={} count={} symbols=[{}]", userId, topEvents.size(), symbols);
            meterRegistry.counter("notification_insight_sent_total", "service", "notification-service").increment();

            for (PendingInsightEvent event : userEvents) {
                event.setProcessed(true);
            }
            pendingInsightEventRepository.saveAll(userEvents);
    }

    private String formatChangeOrNews(PendingInsightEvent e) {
        if (e.getChangePercent() == null) {
            return "news";
        }
        return formatPercent(e.getChangePercent());
    }

    private String formatPercent(BigDecimal changePercent) {
        return changePercent.multiply(new BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP) + "%";
    }
}
