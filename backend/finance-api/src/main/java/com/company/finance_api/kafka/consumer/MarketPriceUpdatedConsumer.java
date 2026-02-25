package com.company.finance_api.kafka.consumer;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.kafka.MarketDataTopics;
import com.company.finance_api.kafka.event.MarketPriceUpdatedEvent;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.PriceService;
import com.company.finance_api.domain.ProcessedEvent;
import com.company.finance_api.repository.ProcessedEventRepository;
import org.springframework.transaction.annotation.Transactional;
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
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = MarketDataTopics.MARKET_PRICE_UPDATED,
            groupId = "finance-api-market-price-consumer",
            containerFactory = "marketPriceKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(MarketPriceUpdatedEvent event) {

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event ignored: {}", event.eventId());
            return;
        }

        Instrument instrument = instrumentRepository.findBySymbol(event.instrumentSymbol())
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        PriceType priceType = PriceType.valueOf(event.priceType());

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(instrument)
                .priceType(priceType)
                .price(event.price())
                .timestamp(event.occurredAt())
                .build();

        priceService.savePrice(price);

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }
}
