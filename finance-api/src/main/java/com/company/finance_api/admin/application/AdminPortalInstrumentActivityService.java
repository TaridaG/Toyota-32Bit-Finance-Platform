package com.company.finance_api.admin.application;

import com.company.finance_api.repository.InstrumentPriceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin KPI: UTC günü başına kaç farklı instrument'a fiyat satırı düştüğünü ölçer (son 7 gün). */
@Service
public class AdminPortalInstrumentActivityService {

  private static final Logger log =
      LoggerFactory.getLogger(AdminPortalInstrumentActivityService.class);

  private final InstrumentPriceRepository instrumentPriceRepository;

  public AdminPortalInstrumentActivityService(InstrumentPriceRepository instrumentPriceRepository) {
    this.instrumentPriceRepository = instrumentPriceRepository;
  }

  /** Son 7 UTC günü için günlük distinct instrument sayılarını döner. */
  @Transactional(readOnly = true)
  public List<Integer> distinctInstrumentsWithPriceDailyLast7Utc(Instant now) {
    LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
    List<Integer> out = new ArrayList<>(7);
    for (int i = 6; i >= 0; i--) {
      LocalDate d = todayUtc.minusDays(i);
      Instant start = d.atStartOfDay(ZoneOffset.UTC).toInstant();
      Instant end = start.plus(1, ChronoUnit.DAYS);
      try {
        long n = instrumentPriceRepository.countDistinctInstrumentsWithPriceBetween(start, end);
        out.add((int) Math.min(Integer.MAX_VALUE, n));
      } catch (RuntimeException ex) {
        log.warn("instrument price activity bucket skipped for day={}", d, ex);
        out.add(0);
      }
    }
    return out;
  }
}
