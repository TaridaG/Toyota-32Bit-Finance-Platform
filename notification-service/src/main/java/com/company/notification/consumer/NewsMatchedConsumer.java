package com.company.notification.consumer;

import com.company.notification.client.FinanceInstrumentLookupService;
import com.company.notification.domain.PendingInsightEvent;
import com.company.notification.domain.WatchlistProjection;
import com.company.notification.event.NewsInstrumentMatchedEvent;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class NewsMatchedConsumer {

    private static final Logger log = LoggerFactory.getLogger(NewsMatchedConsumer.class);

    private final WatchlistProjectionService watchlistProjectionService;
    private final PendingInsightEventRepository pendingInsightEventRepository;
    private final FinanceInstrumentLookupService financeInstrumentLookupService;
    private final MeterRegistry meterRegistry;

    public NewsMatchedConsumer(
            WatchlistProjectionService watchlistProjectionService,
            PendingInsightEventRepository pendingInsightEventRepository,
            FinanceInstrumentLookupService financeInstrumentLookupService,
            MeterRegistry meterRegistry
    ) {
        this.watchlistProjectionService = watchlistProjectionService;
        this.pendingInsightEventRepository = pendingInsightEventRepository;
        this.financeInstrumentLookupService = financeInstrumentLookupService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    @KafkaListener(
            topics = "news.instrument.matched",
            groupId = "notification-service-news-matched",
            containerFactory = "newsInstrumentMatchedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, NewsInstrumentMatchedEvent> record) {
        NewsInstrumentMatchedEvent event = record.value();
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
