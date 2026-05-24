package com.company.marketdataservice.spot.infrastructure.kafka;
import com.company.marketdataservice.shared.kafka.MarketDataTopics;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * `spot fiyat` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
@Component
@RequiredArgsConstructor
public class KafkaMarketEventPublisher implements MarketEventPublisher {

    private final KafkaTemplate<String, MarketPriceUpdatedEvent> kafkaTemplate;

    @Override
    public void publishMarketPriceUpdated(MarketPriceUpdatedEvent event) {
        kafkaTemplate.send(MarketDataTopics.MARKET_PRICE_UPDATED, event.instrumentSymbol(), event);
    }
}
