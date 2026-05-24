package com.company.notification.insight.infrastructure.kafka;

import com.company.notification.insight.infrastructure.http.FinanceInstrumentLookupClient;
import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.watchlist.domain.WatchlistProjection;
import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.insight.infrastructure.kafka.messaging.NewsInstrumentMatchedMessage;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * {@code news.instrument.matched} event'lerini consume eder ve watchlist takipçileri için pending news satırlarını kuyruğa alır.
 */
@Component
public class NewsMatchedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NewsMatchedEventConsumer.class);

    private final SyncWatchlistProjectionUseCase watchlistProjectionService;
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final FinanceInstrumentLookupClient financeInstrumentLookupService;
    private final MeterRegistry meterRegistry;

    public NewsMatchedEventConsumer(
            SyncWatchlistProjectionUseCase watchlistProjectionService,
            PendingInsightEventRepository pendingInsightEventRepository,
            FinanceInstrumentLookupClient financeInstrumentLookupService,
            MeterRegistry meterRegistry
    ) {
        this.watchlistProjectionService = watchlistProjectionService;
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.financeInstrumentLookupService = financeInstrumentLookupService;
        this.meterRegistry = meterRegistry;
    }

    /** Symbol'leri instrument'lara çözer ve watchlist takipçisi başına bir pending news satırı kuyruğa alır. */
    @Transactional
    @KafkaListener(
            topics = KafkaTopicNames.NEWS_INSTRUMENT_MATCHED,
            groupId = "notification-service-news-matched",
            containerFactory = "newsInstrumentMatchedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, NewsInstrumentMatchedMessage> record) {
        NewsInstrumentMatchedMessage event = record.value();
        if (event == null || event.getSymbols() == null || event.getSymbols().isEmpty()) {
            return;
        }
        String title = event.getTitle() == null ? "" : event.getTitle();
        Instant occurredAt = event.getPublishedAt() != null ? event.getPublishedAt() : Instant.now();

        for (String rawSymbol : event.getSymbols()) {
            if (rawSymbol == null || rawSymbol.isBlank()) {
                continue;
            }
            String symbol = rawSymbol.trim();
            Optional<Long> instrumentId = financeInstrumentLookupService.resolveInstrumentId(symbol);
            if (instrumentId.isEmpty()) {
                continue;
            }
            List<WatchlistProjection> followers =
                    watchlistProjectionService.findActiveFollowers(instrumentId.get());
            if (followers.isEmpty()) {
                continue;
            }
            for (WatchlistProjection follower : followers) {
                if (title.isEmpty()) {
                    continue;
                }
                if (pendingInsightEventRepository.existsByUserIdAndInstrumentIdAndNewsTitleAndEventType(
                        follower.getUserId(),
                        instrumentId.get(),
                        title,
                        "NEWS"
                )) {
                    meterRegistry.counter("notification_news_duplicate_total", "service", "notification-service")
                            .increment();
                    log.debug(
                            "NEWS_NOTIFICATION_DUPLICATE_SKIPPED userId={} symbol={} title={}",
                            follower.getUserId(),
                            symbol,
                            title
                    );
                    continue;
                }
                PendingInsightEvent pending = new PendingInsightEvent();
                pending.setUserId(follower.getUserId());
                pending.setInstrumentId(instrumentId.get());
                pending.setSymbol(symbol);
                pending.setChangePercent(null);
                pending.setDirection("NONE");
                pending.setOccurredAt(occurredAt);
                pending.setProcessed(false);
                pending.setEventType("NEWS");
                pending.setNewsTitle(title);
                try {
                    pendingInsightEventRepository.save(pending);
                    log.info(
                            "NEWS_NOTIFICATION_CREATED userId={} symbol={} instrumentId={}",
                            follower.getUserId(),
                            symbol,
                            instrumentId.get()
                    );
                    meterRegistry.counter("notification_news_pending_created_total", "service", "notification-service")
                            .increment();
                } catch (DataIntegrityViolationException ex) {
                    meterRegistry.counter("notification_news_duplicate_total", "service", "notification-service")
                            .increment();
                    log.debug(
                            "NEWS_NOTIFICATION_DUPLICATE_SKIPPED userId={} symbol={} reason=constraint",
                            follower.getUserId(),
                            symbol
                    );
                }
            }
        }
    }
}
