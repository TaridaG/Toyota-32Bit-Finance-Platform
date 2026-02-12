package com.company.finance_api.kafka.consumer;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.kafka.MarketDataTopics;
import com.company.finance_api.kafka.event.MarketPriceUpdatedEvent;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceUpdatedConsumer {

    private final InstrumentRepository instrumentRepository;
    private final PriceService priceService;

    @KafkaListener(
            topics = MarketDataTopics.MARKET_PRICE_UPDATED,
            groupId = "finance-api-market-price-consumer",
            containerFactory = "marketPriceKafkaListenerContainerFactory"
    )
    public void handle(MarketPriceUpdatedEvent event) {

        Instrument instrument = instrumentRepository.findBySymbol(event.instrumentSymbol())
                .orElseThrow(() -> new IllegalStateException(
                        "Instrument not found for symbol=" + event.instrumentSymbol()
                ));

        PriceType priceType;
        try {
            priceType = PriceType.valueOf(event.priceType());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown priceType received: {}, symbol={}", event.priceType(), event.instrumentSymbol(), ex);
            return; // Geçersiz mesajı sessizce drop et
        }

        InstrumentPrice price = new InstrumentPrice(
                instrument,
                priceType,
                event.price(),
                event.occurredAt() != null ? event.occurredAt() : Instant.now()
        );

        priceService.savePrice(price);
        log.info("MARKET_PRICE_CONSUMED symbol={}, price={}, source={}",
                event.instrumentSymbol(), event.price(), event.source());
    }
}
