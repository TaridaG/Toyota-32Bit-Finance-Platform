package com.company.notification.insight.infrastructure.persistence;

import com.company.notification.insight.domain.PendingInsightEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Toplu teslimat bekleyen insight ve haber bildirimlerini kalıcı olarak saklar.
 */
public interface PendingInsightEventRepository extends JpaRepository<PendingInsightEvent, Long> {

    /** {@link com.company.notification.insight.infrastructure.scheduler.InsightAggregationScheduler} tarafından henüz işlenmemiş satırları döner. */
    List<PendingInsightEvent> findByProcessedFalse();

    /** Aynı kullanıcı, instrument ve başlık için duplicate haber enqueue'u tespit eder. */
    boolean existsByUserIdAndInstrumentIdAndNewsTitleAndEventType(
            UUID userId,
            Long instrumentId,
            String newsTitle,
            String eventType
    );
}
