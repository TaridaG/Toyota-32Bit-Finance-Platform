package com.company.marketdataservice.kafka;

import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMarketEventPublisher implements MarketEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishMarketPriceUpdated(MarketPriceUpdatedEvent event) {
        kafkaTemplate.send(MarketDataTopics.MARKET_PRICE_UPDATED, event.instrumentSymbol(), event);
    }
}
