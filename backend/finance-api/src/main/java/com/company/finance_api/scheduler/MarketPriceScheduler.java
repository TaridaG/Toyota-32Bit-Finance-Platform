package com.company.finance_api.scheduler;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.PriceService;
import com.company.finance_api.service.price.PriceProvider;
import com.company.finance_api.service.price.PriceProviderResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceScheduler {

    private final InstrumentRepository instrumentRepository;
    private final PriceProviderResolver providerResolver;
    private final PriceService priceService;

    @Scheduled(fixedDelayString = "${scheduler.market-price.delay-ms}")
    public void refreshMarketPrices() {

        List<Instrument> instruments =
                instrumentRepository.findByActiveTrue();

        PriceProvider provider =
                providerResolver.resolve(PriceType.MARKET);

        for (Instrument instrument : instruments) {

            InstrumentPrice price =
                    provider.fetchLatestPrice(instrument);

            priceService.savePrice(price);

            log.debug(
                    "Market price updated: {} = {}",
                    instrument.getSymbol(),
                    price.getPrice()
            );
        }
    }
}