package com.company.marketdataservice.scheduler;

import com.company.marketdataservice.config.MarketDataProperties;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.kafka.MarketEventPublisher;
import com.company.marketdataservice.provider.PriceProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketScheduler {

    private final MarketDataProperties properties;
    private final PriceProvider priceProvider;
    private final MarketEventPublisher publisher;

    @Scheduled(fixedDelayString = "${scheduler.market.delay-ms:5000}")
    public void pullMarketData() {


        for (String symbol : properties.getTrackedSymbols()) {

            try {

                BigDecimal price = priceProvider.fetchPrice(symbol);

                publisher.publishMarketPriceUpdated(
                        MarketPriceUpdatedEvent.of(
                                symbol,
                                price,
                                "MARKET",
                                priceProvider.source()
                        )
                );

                log.info("MARKET_DATA_PUBLISHED source={}, symbol={}, price={}",
                        priceProvider.source(), symbol, price);

            } catch (Exception e) {

                log.error("MARKET_DATA_ERROR symbol={}, error={}",
                        symbol, e.getMessage());

            }
        }
    }
}
