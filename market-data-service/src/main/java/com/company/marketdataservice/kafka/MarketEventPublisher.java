package com.company.marketdataservice.kafka;

import com.company.marketdataservice.event.MarketPriceUpdatedEvent;

public interface MarketEventPublisher {
    void publishMarketPriceUpdated(MarketPriceUpdatedEvent event);
}
