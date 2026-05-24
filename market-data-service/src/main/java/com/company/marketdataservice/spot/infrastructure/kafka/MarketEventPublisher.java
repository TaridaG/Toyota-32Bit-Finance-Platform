package com.company.marketdataservice.spot.infrastructure.kafka;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;

/**
 * `spot fiyat` domain event'lerini Kafka topic'ine publish eden adaptör.
 */
public interface MarketEventPublisher {
    void publishMarketPriceUpdated(MarketPriceUpdatedEvent event);
}
