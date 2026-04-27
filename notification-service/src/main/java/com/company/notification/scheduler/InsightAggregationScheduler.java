package com.company.notification.scheduler;

import com.company.notification.domain.PendingInsightEvent;
import com.company.notification.repository.PendingInsightEventRepository;
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

@Component
public class InsightAggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightAggregationScheduler.class);
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final MeterRegistry meterRegistry;

    public InsightAggregationScheduler(PendingInsightEventRepository pendingInsightEventRepository, MeterRegistry meterRegistry) {
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.meterRegistry = meterRegistry;
    }

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
            List<PendingInsightEvent> topEvents = entry.getValue().stream()
                    .sorted(Comparator.comparing(e -> e.getChangePercent().abs(), Comparator.reverseOrder()))
                    .limit(5)
                    .toList();

            String symbols = topEvents.stream()
                    .map(e -> e.getSymbol() + " " + formatPercent(e.getChangePercent()))
                    .collect(Collectors.joining(","));

            log.info("INSIGHT_NOTIFICATION userId={} count={} symbols=[{}]", entry.getKey(), topEvents.size(), symbols);
            meterRegistry.counter("notification_insight_sent_total", "service", "notification-service").increment();

            for (PendingInsightEvent event : entry.getValue()) {
                event.setProcessed(true);
            }
            pendingInsightEventRepository.saveAll(entry.getValue());
        }
    }

    private String formatPercent(BigDecimal changePercent) {
        return changePercent.multiply(new BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP) + "%";
    }
}
