package com.company.finance_api.service.price;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;

@Service
@Profile({"dev", "kafka"})
public class MockPriceProvider implements PriceProvider {

    private final Random random = new Random();

    @Override
    public PriceType supports() {
        return PriceType.MARKET;
    }

    @Override
    public InstrumentPrice fetchLatestPrice(Instrument instrument) {
        BigDecimal price =
                BigDecimal.valueOf(100 + random.nextInt(50));

        return InstrumentPrice.builder()
                .instrument(instrument)
                .price(price)
                .priceType(PriceType.MARKET)
                .timestamp(Instant.now())
                .build();
    }
}