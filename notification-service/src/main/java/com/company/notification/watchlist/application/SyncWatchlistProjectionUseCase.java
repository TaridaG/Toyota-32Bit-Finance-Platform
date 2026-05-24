package com.company.notification.watchlist.application;

import com.company.notification.watchlist.domain.NotificationProcessedEvent;
import com.company.notification.watchlist.domain.WatchlistProjection;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemAddedMessage;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemRemovedMessage;
import com.company.notification.watchlist.infrastructure.persistence.NotificationProcessedEventRepository;
import com.company.notification.watchlist.infrastructure.persistence.WatchlistProjectionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bildirim fan-out için watchlist üyeliğinin yerel read model'ini günceller.
 * Idempotent apply, stale event'ler ve duplicate Kafka delivery'lerini yönetir.
 */
@Service
public class SyncWatchlistProjectionUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncWatchlistProjectionUseCase.class);
    private final WatchlistProjectionRepository watchlistProjectionRepository;
    private final NotificationProcessedEventRepository notificationProcessedEventRepository;
    private final MeterRegistry meterRegistry;

    public SyncWatchlistProjectionUseCase(
            WatchlistProjectionRepository watchlistProjectionRepository,
            NotificationProcessedEventRepository notificationProcessedEventRepository,
            MeterRegistry meterRegistry
    ) {
        this.watchlistProjectionRepository = watchlistProjectionRepository;
        this.notificationProcessedEventRepository = notificationProcessedEventRepository;
        this.meterRegistry = meterRegistry;
        Gauge.builder("watchlist_projection_active_items_gauge", watchlistProjectionRepository, WatchlistProjectionRepository::countByActiveTrue)
                .tag("service", "notification-service")
                .register(meterRegistry);
    }

    /**
     * Bir instrument watchlist'e eklendiğinde projection satırını aktive eder veya oluşturur.
     */
    @Transactional
    public void applyAdded(WatchlistItemAddedMessage event) {
        if (event == null || event.getEventId() == null) {
            return;
        }
        if (notificationProcessedEventRepository.existsById(event.getEventId())) {
            incrementResult("added", "duplicate");
            log.debug("watchlist_projection_duplicate eventType=added eventId={}", event.getEventId());
            return;
        }
        Instant occurredAt = resolveOccurredAt(event.getOccurredAt());
        WatchlistProjection projection = watchlistProjectionRepository
                .findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId())
                .orElse(null);

        if (projection != null && projection.getUpdatedAt() != null && occurredAt.isBefore(projection.getUpdatedAt())) {
            markProcessed(event.getEventId(), "watchlist.item.added");
            incrementResult("added", "stale");
            log.warn("watchlist_projection_stale eventType=added eventId={}", event.getEventId());
            return;
        }

        WatchlistProjection target = projection == null ? new WatchlistProjection() : projection;
        target.setUserId(event.getUserId());
        target.setInstrumentId(event.getInstrumentId());
        target.setSymbol(event.getSymbol() == null ? "" : event.getSymbol());
        target.setActive(true);
        target.setUpdatedAt(occurredAt);
        watchlistProjectionRepository.save(target);
        markProcessed(event.getEventId(), "watchlist.item.added");
        incrementResult("added", "applied");
        log.info("watchlist_projection_applied eventType=added eventId={}", event.getEventId());
    }

    /**
     * Bir instrument watchlist'ten kaldırıldığında projection satırını deaktive eder.
     */
    @Transactional
    public void applyRemoved(WatchlistItemRemovedMessage event) {
        if (event == null || event.getEventId() == null) {
            return;
        }
        if (notificationProcessedEventRepository.existsById(event.getEventId())) {
            incrementResult("removed", "duplicate");
            log.debug("watchlist_projection_duplicate eventType=removed eventId={}", event.getEventId());
            return;
        }
        Instant occurredAt = resolveOccurredAt(event.getOccurredAt());
        WatchlistProjection projection = watchlistProjectionRepository
                .findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId())
                .orElse(null);
        if (projection == null) {
            markProcessed(event.getEventId(), "watchlist.item.removed");
            incrementResult("removed", "noop");
            return;
        }
        if (projection.getUpdatedAt() != null && occurredAt.isBefore(projection.getUpdatedAt())) {
            markProcessed(event.getEventId(), "watchlist.item.removed");
            incrementResult("removed", "stale");
            log.warn("watchlist_projection_stale eventType=removed eventId={}", event.getEventId());
            return;
        }
        projection.setActive(false);
        projection.setUpdatedAt(occurredAt);
        watchlistProjectionRepository.save(projection);
        markProcessed(event.getEventId(), "watchlist.item.removed");
        incrementResult("removed", "applied");
        log.info("watchlist_projection_applied eventType=removed eventId={}", event.getEventId());
    }

    /**
     * Verilen instrument'ı şu an takip eden kullanıcıları döner (insight/haber fan-out için).
     */
    @Transactional(readOnly = true)
    public List<WatchlistProjection> findActiveFollowers(Long instrumentId) {
        return watchlistProjectionRepository.findByInstrumentIdAndActiveTrue(instrumentId);
    }

    private void markProcessed(UUID eventId, String eventType) {
        NotificationProcessedEvent processedEvent = new NotificationProcessedEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setEventType(eventType);
        processedEvent.setProcessedAt(Instant.now());
        notificationProcessedEventRepository.save(processedEvent);
    }

    private void incrementResult(String eventType, String result) {
        meterRegistry.counter(
                "watchlist_projection_applied_total",
                "eventType", eventType,
                "result", result
        ).increment();
    }

    private Instant resolveOccurredAt(Instant occurredAt) {
        return occurredAt == null ? Instant.now() : occurredAt;
    }
}
