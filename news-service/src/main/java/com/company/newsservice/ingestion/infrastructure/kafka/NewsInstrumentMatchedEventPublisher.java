package com.company.newsservice.ingestion.infrastructure.kafka;

import com.company.newsservice.bootstrap.config.kafka.KafkaTopicNames;
import com.company.newsservice.ingestion.infrastructure.kafka.messaging.NewsInstrumentMatchedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Enstrüman eşleşmesi bulunan haberler için Kafka'ya {@code news.instrument.matched} event'i publish eder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsInstrumentMatchedEventPublisher {

    private final KafkaTemplate<String, Object> newsKafkaTemplate;

    /**
     * Eşleşen semboller ve makale metadata'sı ile Kafka event'i publish eder.
     *
     * @param articleUrl makale URL'si (Kafka message key)
     * @param matchedSymbols eşleşen enstrüman sembolleri
     * @param title makale başlığı
     * @param sourceName kaynak adı
     * @param publishedAt yayın zamanı
     */
    public void publish(String articleUrl, List<String> matchedSymbols, String title, String sourceName, Instant publishedAt) {
        try {
            NewsInstrumentMatchedMessage payload =
                    NewsInstrumentMatchedMessage.of(matchedSymbols, title, sourceName, publishedAt);
            newsKafkaTemplate.send(KafkaTopicNames.NEWS_INSTRUMENT_MATCHED, articleUrl, payload);
        } catch (Exception ex) {
            log.warn("NEWS_INSTRUMENT_MATCHED_PUBLISH_FAILED title={} reason={}", title, ex.toString());
        }
    }
}
