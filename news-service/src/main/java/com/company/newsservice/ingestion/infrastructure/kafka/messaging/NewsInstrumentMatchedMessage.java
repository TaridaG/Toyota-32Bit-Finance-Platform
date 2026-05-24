package com.company.newsservice.ingestion.infrastructure.kafka.messaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code news.instrument.matched} için payload (notification-service consumer sözleşmesiyle uyumlu).
 */
public class NewsInstrumentMatchedMessage {

    private List<String> symbols = new ArrayList<>();
    private String title;
    private String sourceName;
    private Instant publishedAt;

    /**
     * Eşleşen semboller ve haber metadata'sından Kafka payload'u oluşturur.
     *
     * @param symbols eşleşen enstrüman sembolleri
     * @param title makale başlığı
     * @param sourceName kaynak adı
     * @param publishedAt yayın zamanı
     * @return serialize edilebilir message instance'ı
     */
    public static NewsInstrumentMatchedMessage of(
            List<String> symbols,
            String title,
            String sourceName,
            Instant publishedAt
    ) {
        NewsInstrumentMatchedMessage e = new NewsInstrumentMatchedMessage();
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
