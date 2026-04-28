package com.company.newsservice.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Kafka payload for topic {@code news.instrument.matched} (shape aligned with notification consumer). */
public class NewsInstrumentMatchedEvent {

    private List<String> symbols = new ArrayList<>();
    private String title;
    private String sourceName;
    private Instant publishedAt;

    public static NewsInstrumentMatchedEvent of(
            List<String> symbols,
            String title,
            String sourceName,
            Instant publishedAt
    ) {
        NewsInstrumentMatchedEvent e = new NewsInstrumentMatchedEvent();
        e.setSymbols(symbols == null ? List.of() : symbols);
        e.setTitle(title);
        e.setSourceName(sourceName);
        e.setPublishedAt(publishedAt);
        return e;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols == null ? new ArrayList<>() : new ArrayList<>(symbols);
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
