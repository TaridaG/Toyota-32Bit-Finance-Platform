package com.company.finance_api.scheduler;

import com.company.finance_api.service.PriceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketPriceScheduler {

    private final PriceService priceService;

    public MarketPriceScheduler(PriceService priceService) {
        this.priceService = priceService;
    }

    @Scheduled(fixedDelayString = "${scheduler.market-price.delay-ms:60000}")
    public void fetchMarketPrices() {

    }
}
