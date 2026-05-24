package com.company.newsservice.ingestion.infrastructure.kafka;

import com.company.newsservice.bootstrap.config.kafka.KafkaTopicNames;
import com.company.newsservice.ingestion.infrastructure.kafka.messaging.NewsInstrumentMatchedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewsInstrumentMatchedEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> newsKafkaTemplate;

    @InjectMocks
    private NewsInstrumentMatchedEventPublisher publisher;

    @Test
    void publish_sendsMatchedPayloadToTopic() {
        Instant publishedAt = Instant.parse("2026-05-23T10:00:00Z");

        publisher.publish(
                "https://example.com/btc",
                List.of("BTCUSDT"),
                "Bitcoin rises",
                "Reuters",
                publishedAt
        );

        ArgumentCaptor<NewsInstrumentMatchedMessage> payloadCaptor = ArgumentCaptor.forClass(NewsInstrumentMatchedMessage.class);
        verify(newsKafkaTemplate).send(
                eq(KafkaTopicNames.NEWS_INSTRUMENT_MATCHED),
                eq("https://example.com/btc"),
                payloadCaptor.capture()
        );
        NewsInstrumentMatchedMessage payload = payloadCaptor.getValue();
        assertEquals(List.of("BTCUSDT"), payload.getSymbols());
        assertEquals("Bitcoin rises", payload.getTitle());
        assertEquals("Reuters", payload.getSourceName());
        assertEquals(publishedAt, payload.getPublishedAt());
    }

    @Test
    void publish_swallowsKafkaFailure() {
        doThrow(new RuntimeException("broker down"))
                .when(newsKafkaTemplate)
                .send(eq(KafkaTopicNames.NEWS_INSTRUMENT_MATCHED), eq("url"), org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(() -> publisher.publish("url", List.of("ETHUSDT"), "t", "s", Instant.now()));
    }
}
