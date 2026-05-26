package com.company.marketdataservice.rates.application;

import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositDailyIndexEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositDailyIndexRepository;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class TlDepositDailyIndexSyncService {

  private static final ZoneId TR = ZoneId.of("Europe/Istanbul");
  private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final double DAY_COUNT = 365.25d;

  private final TlDepositWeeklyRepository tlDepositWeeklyRepository;
  private final TlDepositDailyIndexRepository tlDepositDailyIndexRepository;
  private final AtomicBoolean asyncKickoffInFlight = new AtomicBoolean(false);

  public TlDepositDailyIndexSyncService(
      TlDepositWeeklyRepository tlDepositWeeklyRepository,
      TlDepositDailyIndexRepository tlDepositDailyIndexRepository) {
    this.tlDepositWeeklyRepository = tlDepositWeeklyRepository;
    this.tlDepositDailyIndexRepository = tlDepositDailyIndexRepository;
  }

  public void requestInitialBuildIfEmptyAsync(Set<String> maturities) {
    if (maturities == null || maturities.isEmpty()) {
      return;
    }
    boolean anyMissing =
        maturities.stream().anyMatch(maturity -> tlDepositDailyIndexRepository.countByMaturityCode(maturity) == 0);
    if (!anyMissing) {
      return;
    }
    if (!asyncKickoffInFlight.compareAndSet(false, true)) {
      return;
    }
    Thread.ofVirtual()
        .name("mds-tl-deposit-daily-index-initial")
        .start(
            () -> {
              try {
                rebuildForMaturities(maturities);
              } finally {
                asyncKickoffInFlight.set(false);
              }
            });
  }

  public void rebuildForMaturities(Set<String> maturities) {
    if (maturities == null || maturities.isEmpty()) {
      return;
    }
    for (String maturity : maturities) {
      String normalized = maturity.trim().toUpperCase(Locale.ROOT);
      List<TlDepositWeeklyEntity> weeklyRows =
          tlDepositWeeklyRepository.findByMaturityCodeAndWeekStartBetweenOrderByWeekStartAsc(
              normalized, LocalDate.now(TR).minusYears(5).minusYears(1), LocalDate.now(TR));
      rebuildSingle(normalized, weeklyRows);
    }
  }

  void rebuildSingle(String maturity, List<TlDepositWeeklyEntity> weeklyRows) {
    if (weeklyRows == null || weeklyRows.isEmpty()) {
      return;
    }
    List<TlDepositDailyIndexEntity> dailyRows = buildDailyRows(maturity, weeklyRows, LocalDate.now(TR));
    if (dailyRows.isEmpty()) {
      return;
    }
    LocalDate from = dailyRows.getFirst().getDay();
    LocalDate to = dailyRows.getLast().getDay();
    tlDepositDailyIndexRepository.deleteByMaturityCodeAndDayBetween(maturity, from, to);
    tlDepositDailyIndexRepository.saveAll(dailyRows);
  }

  static List<TlDepositDailyIndexEntity> buildDailyRows(
      String maturity, List<TlDepositWeeklyEntity> weeklyRows, LocalDate today) {
    List<TlDepositWeeklyEntity> ordered =
        weeklyRows.stream()
            .sorted(Comparator.comparing(TlDepositWeeklyEntity::getWeekStart))
            .toList();
    if (ordered.isEmpty()) {
      return List.of();
    }
    List<EffectiveRateRow> effectiveRows = new ArrayList<>(ordered.size());
    for (TlDepositWeeklyEntity row : ordered) {
      LocalDate effectiveDate =
          row.getSourceObservationDate() != null ? row.getSourceObservationDate() : row.getWeekStart().plusDays(6);
      if (effectiveDate == null || effectiveDate.isAfter(today)) {
        continue;
      }
      effectiveRows.add(
          new EffectiveRateRow(
              effectiveDate,
              row.getRatePercent(),
              row.getSourceObservationDate(),
              row.getSourceProvider()));
    }
    if (effectiveRows.isEmpty()) {
      return List.of();
    }
    effectiveRows.sort(Comparator.comparing(EffectiveRateRow::effectiveDate));
    List<TlDepositDailyIndexEntity> out = new ArrayList<>();
    int idx = 0;
    EffectiveRateRow current = effectiveRows.get(0);
    LocalDate start = current.effectiveDate();
    BigDecimal indexValue = ONE.setScale(10, RoundingMode.HALF_UP);
    Instant now = Instant.now();
    for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
      while (idx + 1 < effectiveRows.size() && !effectiveRows.get(idx + 1).effectiveDate().isAfter(day)) {
        idx++;
        current = effectiveRows.get(idx);
      }
      if (!day.equals(start)) {
        indexValue = indexValue.multiply(dailyFactor(current.annualRatePercent()), MC).setScale(10, RoundingMode.HALF_UP);
      }
      TlDepositDailyIndexEntity row = new TlDepositDailyIndexEntity();
      row.setDay(day);
      row.setMaturityCode(maturity);
      row.setIndexValue(indexValue);
      row.setAnnualRatePercent(current.annualRatePercent().setScale(4, RoundingMode.HALF_UP));
      row.setSourceObservationDate(current.sourceObservationDate());
      row.setSourceProvider(current.sourceProvider() != null ? current.sourceProvider() : "TCMB_EVDS");
      row.setComputedAt(now);
      out.add(row);
    }
    return out;
  }

  private static BigDecimal dailyFactor(BigDecimal annualRatePercent) {
    double annualRate = annualRatePercent.doubleValue() / 100d;
    double factor = Math.pow(1d + annualRate, 1d / DAY_COUNT);
    return BigDecimal.valueOf(factor);
  }

  private record EffectiveRateRow(
      LocalDate effectiveDate,
      BigDecimal annualRatePercent,
      LocalDate sourceObservationDate,
      String sourceProvider) {}
}
