package com.company.marketdataservice.scheduler;

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
public class BinanceMarketScheduler {

    private final PriceProvider binancePriceProvider; // Spring otomatik inject eder
    private final MarketEventPublisher publisher;

    // İlk adım: sadece BTCUSDT
    @Scheduled(fixedDelayString = "${scheduler.binance.delay-ms:5000}")
    public void pullBtcUsdt() {

        String symbol = "BTCUSDT";

        BigDecimal price = binancePriceProvider.fetchPrice(symbol);

        publisher.publishMarketPriceUpdated(
                MarketPriceUpdatedEvent.of(symbol, price, "MARKET", binancePriceProvider.source())
        );

        log.info("MARKET_DATA_PUBLISHED source={}, symbol={}, price={}",
                binancePriceProvider.source(), symbol, price);
    }
}
