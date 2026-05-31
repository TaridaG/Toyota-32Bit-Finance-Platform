package com.company.finance_api.pricing.infrastructure.provider;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** MockPriceProvider iş mantığını uygular (mock price provider). */
@Service
@Profile({"dev", "kafka"})
public class MockPriceProvider implements PriceProvider {

  private final Random random = new Random();

  /** supports işlemini gerçekleştirir. */
  @Override
  public PriceType supports() {
    return PriceType.MARKET;
  }

  /** Enstrüman için son fiyat snapshot'ını provider'dan çeker. */
  @Override
  public InstrumentPrice fetchLatestPrice(Instrument instrument) {
    BigDecimal price = BigDecimal.valueOf(100 + random.nextInt(50));

    return InstrumentPrice.builder()
        .instrument(instrument)
        .price(price)
        .priceType(PriceType.MARKET)
        .timestamp(Instant.now())
        .build();
  }
}
