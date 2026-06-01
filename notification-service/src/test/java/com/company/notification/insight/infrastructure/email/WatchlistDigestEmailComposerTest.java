package com.company.notification.insight.infrastructure.email;

import com.company.notification.bootstrap.config.NotificationMailProperties;
import com.company.notification.insight.domain.PendingInsightEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WatchlistDigestEmailComposerTest {

    private final WatchlistDigestEmailComposer composer =
            new WatchlistDigestEmailComposer(new NotificationMailProperties());

    @Test
    void build_tr_subject_and_insight_line() {
        PendingInsightEvent insight = new PendingInsightEvent();
        insight.setSymbol("AAPL");
        insight.setChangePercent(new BigDecimal("0.12"));
        insight.setDirection("UP");
        insight.setEventType("INSIGHT");

        WatchlistDigestEmailContent content = composer.build("tr", List.of(insight));

        assertTrue(content.subject().contains("Takip"));
        assertTrue(content.plainBody().contains("AAPL"));
        assertTrue(content.plainBody().contains("Yükseliş"));
        assertTrue(content.htmlBody().contains("AAPL"));
    }

    @Test
    void build_en_news_line() {
        PendingInsightEvent news = new PendingInsightEvent();
        news.setUserId(UUID.randomUUID());
        news.setSymbol("THYAO");
        news.setEventType("NEWS");
        news.setNewsTitle("Market headline");
        news.setOccurredAt(Instant.now());

        WatchlistDigestEmailContent content = composer.build("en", List.of(news));

        assertTrue(content.subject().toLowerCase().contains("watchlist"));
        assertTrue(content.plainBody().contains("News:"));
        assertTrue(content.plainBody().contains("Market headline"));
    }
}
