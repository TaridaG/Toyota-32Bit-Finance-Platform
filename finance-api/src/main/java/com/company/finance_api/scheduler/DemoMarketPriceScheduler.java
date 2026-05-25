package com.company.finance_api.scheduler;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.alarm.application.AlarmService;
import com.company.finance_api.service.PriceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Demo ortamında sentetik piyasa fiyatı üretimini zamanlar. */
@Component
@Profile("demo-prices")
public class DemoMarketPriceScheduler {

  private static final Logger log = LoggerFactory.getLogger(DemoMarketPriceScheduler.class);

  private final InstrumentRepository instrumentRepository;
  private final PriceService priceService;
  private final AlarmService alarmService;

  private final Random random = new Random();

  public DemoMarketPriceScheduler(
      InstrumentRepository instrumentRepository,
      PriceService priceService,
      AlarmService alarmService) {
    this.instrumentRepository = instrumentRepository;
    this.priceService = priceService;
    this.alarmService = alarmService;
  }

  //  PROD DEĞİL — sadece DEMO AMAÇLI
  @Scheduled(fixedDelay = 10_000)
  public void simulateMarketPrice() {

    List<Instrument> instruments = instrumentRepository.findAll();
    if (instruments.isEmpty()) {
      return;
    }

    Instrument instrument = instruments.get(random.nextInt(instruments.size()));

    BigDecimal price = BigDecimal.valueOf(100 + random.nextInt(50));

    InstrumentPrice instrumentPrice =
        new InstrumentPrice(instrument, PriceType.LAST, price, Instant.now());

    priceService.savePrice(instrumentPrice);
  }
}
