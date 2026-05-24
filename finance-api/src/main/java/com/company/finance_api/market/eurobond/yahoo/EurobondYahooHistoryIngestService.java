package com.company.finance_api.market.eurobond.yahoo;

import com.company.finance_api.bootstrap.config.MarketTrUsdEurobondYahooProperties;
import com.company.finance_api.bootstrap.config.MarketTrUsdEurobondYahooProperties.InstrumentRow;
import com.company.finance_api.domain.EurobondHistory;
import com.company.finance_api.domain.EurobondInstrument;
import com.company.finance_api.domain.EurobondQuote;
import com.company.finance_api.market.eurobond.SemiAnnualBondYieldSolver;
import com.company.finance_api.repository.EurobondHistoryRepository;
import com.company.finance_api.repository.EurobondInstrumentRepository;
import com.company.finance_api.repository.EurobondQuoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * EurobondYahooHistoryIngestService iş mantığını uygular (eurobond yahoo history ingest service).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EurobondYahooHistoryIngestService {

  private final MarketTrUsdEurobondYahooProperties properties;
  private final YahooEurobondChartClient chartClient;
  private final EurobondInstrumentRepository instrumentRepository;
  private final EurobondHistoryRepository historyRepository;
  private final EurobondQuoteRepository quoteRepository;

  @Transactional
  public void refreshAllConfigured() {
    for (InstrumentRow row : properties.getInstruments()) {
      try {
        refreshOne(row);
      } catch (Exception ex) {
        log.warn(
            "EUROBOND_YAHOO_REFRESH_FAIL isin={} reason={}",
            row != null ? row.getIsin() : null,
            ex.toString());
      }
      sleepSpacing();
    }
  }

  @Transactional
  public void refreshOne(InstrumentRow row) {
    if (row == null
        || !StringUtils.hasText(row.getIsin())
        || !StringUtils.hasText(row.getYahooChartSymbol())) {
      log.info("EUROBOND_YAHOO_SKIP reason=no_symbol isin={}", row != null ? row.getIsin() : null);
      return;
    }
    String isin = row.getIsin().trim().toUpperCase(Locale.ROOT);
    String yahoo = row.getYahooChartSymbol().trim();
    ZoneId zone = ZoneId.of(properties.getTradingTimezone());
    LocalDate today = LocalDate.now(zone);
    if (historyRepository.existsByIsinAndHistoryDate(isin, today)) {
      log.debug("EUROBOND_YAHOO_SKIP reason=already_have_today isin={} day={}", isin, today);
      return;
    }
    EurobondInstrument inst = instrumentRepository.findById(isin).orElse(null);
    if (inst == null) {
      log.warn("EUROBOND_YAHOO_SKIP reason=no_instrument_row isin={}", isin);
      return;
    }
    String range =
        historyRepository.countByIsin(isin) == 0
            ? properties.getBackfillRange()
            : properties.getCatchUpRange();
    String source = properties.getSourceProviderId();
    YahooEurobondChartResponse resp =
        chartClient.fetchHistoricalChart(yahoo, range, properties.getInterval());
    if (resp == null || resp.chart() == null || resp.chart().error() != null) {
      log.warn(
          "EUROBOND_YAHOO_EMPTY isin={} yahoo={} err={}",
          isin,
          yahoo,
          resp != null && resp.chart() != null ? resp.chart().error() : null);
      return;
    }
    List<Bar> bars = parseBars(resp, zone);
    if (bars.isEmpty()) {
      log.warn("EUROBOND_YAHOO_NO_POINTS isin={} yahoo={}", isin, yahoo);
      return;
    }
    boolean govYieldSeries = isTrGovUsdBenchmarkYieldSymbol(yahoo);
    if (!govYieldSeries && properties.isNormalizeChartToBondPriceWindow()) {
      bars = normalizeBarsToBondWindow(bars);
    }
    bars.sort(Comparator.comparing(Bar::historyDate));
    for (Bar bar : bars) {
      BigDecimal o;
      BigDecimal h;
      BigDecimal l;
      BigDecimal c;
      BigDecimal yO;
      BigDecimal yH;
      BigDecimal yL;
      BigDecimal yC;
      if (govYieldSeries) {
        yC = EurobondHistory.bd6(bar.close());
        if (yC == null) {
          continue;
        }
        yO = coalesceYield(bar.open(), yC);
        yH = coalesceYield(bar.high(), yC);
        yL = coalesceYield(bar.low(), yC);
        BigDecimal maxY = maxBd(yO, yH, yL, yC);
        BigDecimal minY = minBd(yO, yH, yL, yC);
        c = cleanFromYield(inst, yC, bar.historyDate());
        o = cleanFromYield(inst, yO, bar.historyDate());
        h = cleanFromYield(inst, minY, bar.historyDate());
        l = cleanFromYield(inst, maxY, bar.historyDate());
        if (c == null || o == null || h == null || l == null) {
          continue;
        }
      } else {
        o = EurobondHistory.bd6(bar.open());
        h = EurobondHistory.bd6(bar.high());
        l = EurobondHistory.bd6(bar.low());
        c = EurobondHistory.bd6(bar.close());
        if (c == null) {
          continue;
        }
        yO = yieldFor(inst, o, bar.historyDate());
        yH = yieldFor(inst, h, bar.historyDate());
        yL = yieldFor(inst, l, bar.historyDate());
        yC = yieldFor(inst, c, bar.historyDate());
      }
      BigDecimal chg = null;
      Optional<EurobondHistory> prior =
          historyRepository.findFirstByIsinAndHistoryDateLessThanOrderByHistoryDateDesc(
              isin, bar.historyDate());
      if (prior.isPresent()
          && prior.get().getClosePrice() != null
          && prior.get().getClosePrice().compareTo(BigDecimal.ZERO) != 0) {
        chg =
            c.subtract(prior.get().getClosePrice())
                .divide(prior.get().getClosePrice(), 12, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(6, RoundingMode.HALF_UP);
      }
      EurobondHistory rowEntity =
          historyRepository
              .findByIsinAndHistoryDate(isin, bar.historyDate())
              .orElseGet(() -> EurobondHistory.newIngestRow(isin, bar.historyDate(), source));
      rowEntity.applyIngestSnapshot(o, h, l, c, yO, yH, yL, yC, chg, source);
      historyRepository.save(rowEntity);
    }
    upsertLatestQuote(isin, bars.get(bars.size() - 1), inst, source, zone, govYieldSeries);
    log.info("EUROBOND_YAHOO_DONE isin={} yahoo={} bars={}", isin, yahoo, bars.size());
  }

  private void upsertLatestQuote(
      String isin,
      Bar last,
      EurobondInstrument inst,
      String source,
      ZoneId zone,
      boolean govYieldSeries) {
    BigDecimal close;
    BigDecimal ytm;
    if (govYieldSeries) {
      BigDecimal y = EurobondHistory.bd6(last.close());
      if (y == null) {
        return;
      }
      ytm = y;
      close = cleanFromYield(inst, y, last.historyDate());
      if (close == null) {
        return;
      }
    } else {
      close = EurobondHistory.bd6(last.close());
      if (close == null) {
        return;
      }
      ytm = yieldFor(inst, close, last.historyDate());
    }
    Instant quoteTime =
        last.observedAt() != null
            ? last.observedAt()
            : last.historyDate().atStartOfDay(zone).toInstant();
    BigDecimal dailyChg = null;
    List<EurobondHistory> two = historyRepository.findTop2ByIsinOrderByHistoryDateDesc(isin);
    if (two.size() >= 2
        && two.get(0).getClosePrice() != null
        && two.get(1).getClosePrice() != null) {
      BigDecimal c0 = two.get(0).getClosePrice();
      BigDecimal c1 = two.get(1).getClosePrice();
      if (c1.compareTo(BigDecimal.ZERO) != 0) {
        dailyChg =
            c0.subtract(c1)
                .divide(c1, 12, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(6, RoundingMode.HALF_UP);
      }
    }
    quoteRepository.save(
        EurobondQuote.newSnapshot(isin, close, null, null, ytm, dailyChg, quoteTime, source));
  }

  private List<Bar> normalizeBarsToBondWindow(List<Bar> raw) {
    double minV = Double.POSITIVE_INFINITY;
    double maxV = Double.NEGATIVE_INFINITY;
    for (Bar b : raw) {
      for (Double v : List.of(b.open(), b.high(), b.low(), b.close())) {
        if (v != null && Double.isFinite(v)) {
          minV = Math.min(minV, v);
          maxV = Math.max(maxV, v);
        }
      }
    }
    if (!Double.isFinite(minV) || maxV - minV < 1e-9d) {
      return raw;
    }
    double tLo = properties.getNormalizeTargetLow();
    double tHi = properties.getNormalizeTargetHigh();
    List<Bar> out = new ArrayList<>(raw.size());
    for (Bar b : raw) {
      out.add(
          new Bar(
              b.historyDate(),
              b.observedAt(),
              mapToBondWindow(b.open(), minV, maxV, tLo, tHi),
              mapToBondWindow(b.high(), minV, maxV, tLo, tHi),
              mapToBondWindow(b.low(), minV, maxV, tLo, tHi),
              mapToBondWindow(b.close(), minV, maxV, tLo, tHi)));
    }
    return out;
  }

  private static Double mapToBondWindow(
      Double v, double minV, double maxV, double tLo, double tHi) {
    if (v == null || !Double.isFinite(v)) {
      return null;
    }
    double r = (v - minV) / (maxV - minV);
    return tLo + r * (tHi - tLo);
  }

  /** Yahoo GTUSDTR*n*Y:GOV series values are benchmark yields (%), not bond clean prices. */
  private static boolean isTrGovUsdBenchmarkYieldSymbol(String yahoo) {
    String u = yahoo.toUpperCase(Locale.ROOT);
    return u.startsWith("GTUSDTR") && u.endsWith(":GOV");
  }

  private static BigDecimal coalesceYield(Double raw, BigDecimal fallback) {
    BigDecimal v = EurobondHistory.bd6(raw);
    return v != null ? v : fallback;
  }

  private static BigDecimal maxBd(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d) {
    BigDecimal m = a;
    for (BigDecimal x : List.of(b, c, d)) {
      if (x != null && (m == null || x.compareTo(m) > 0)) {
        m = x;
      }
    }
    return m != null ? m : a;
  }

  private static BigDecimal minBd(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d) {
    BigDecimal m = a;
    for (BigDecimal x : List.of(b, c, d)) {
      if (x != null && (m == null || x.compareTo(m) < 0)) {
        m = x;
      }
    }
    return m != null ? m : a;
  }

  private BigDecimal cleanFromYield(
      EurobondInstrument inst, BigDecimal annualYieldPct, LocalDate asOf) {
    if (annualYieldPct == null) {
      return null;
    }
    if (!"SEMI_ANNUAL".equalsIgnoreCase(inst.getCouponFrequency())) {
      return null;
    }
    return SemiAnnualBondYieldSolver.cleanPriceFromAnnualYieldPercent(
        annualYieldPct, inst.getCouponPercent(), asOf, inst.getMaturityDate());
  }

  private BigDecimal yieldFor(EurobondInstrument inst, BigDecimal cleanPrice, LocalDate asOf) {
    if (cleanPrice == null) {
      return null;
    }
    if (!"SEMI_ANNUAL".equalsIgnoreCase(inst.getCouponFrequency())) {
      return null;
    }
    return SemiAnnualBondYieldSolver.annualPercentFromCleanPrice(
        cleanPrice, inst.getCouponPercent(), asOf, inst.getMaturityDate());
  }

  private List<Bar> parseBars(YahooEurobondChartResponse resp, ZoneId zone) {
    if (resp.chart().result() == null || resp.chart().result().isEmpty()) {
      return List.of();
    }
    YahooEurobondChartResponse.Result result = resp.chart().result().get(0);
    if (result.timestamp() == null
        || result.timestamp().isEmpty()
        || result.indicators() == null
        || result.indicators().quote() == null
        || result.indicators().quote().isEmpty()) {
      return List.of();
    }
    YahooEurobondChartResponse.Quote q = result.indicators().quote().get(0);
    List<Double> opens = q.open();
    List<Double> highs = q.high();
    List<Double> lows = q.low();
    List<Double> closes = q.close();
    List<Bar> out = new ArrayList<>();
    for (int i = 0; i < result.timestamp().size(); i++) {
      Long epoch = result.timestamp().get(i);
      if (epoch == null) {
        continue;
      }
      Instant at = Instant.ofEpochSecond(epoch);
      LocalDate day = at.atZone(zone).toLocalDate();
      Double o = idx(opens, i);
      Double h = idx(highs, i);
      Double l = idx(lows, i);
      Double c = idx(closes, i);
      if (c == null || !Double.isFinite(c)) {
        continue;
      }
      out.add(new Bar(day, at, o, h, l, c));
    }
    return out;
  }

  private static Double idx(List<Double> list, int i) {
    if (list == null || i >= list.size()) {
      return null;
    }
    return list.get(i);
  }

  private void sleepSpacing() {
    long ms = Math.max(0L, properties.getRequestSpacingMs());
    if (ms == 0L) {
      return;
    }
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private record Bar(
      LocalDate historyDate,
      Instant observedAt,
      Double open,
      Double high,
      Double low,
      Double close) {}
}
