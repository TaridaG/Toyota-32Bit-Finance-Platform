package com.company.notification.insight.infrastructure.kafka.messaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code news.instrument.matched} için payload (news-service sözleşmesini yansıtır).
 */
public class NewsInstrumentMatchedMessage {

    private List<String> symbols = new ArrayList<>();
    private String title;
    private String sourceName;
    private Instant publishedAt;

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols == null ? new ArrayList<>() : symbols;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
