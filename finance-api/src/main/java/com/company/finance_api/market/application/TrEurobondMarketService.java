package com.company.finance_api.market.application;

import com.company.finance_api.market.domain.EurobondHistory;
import com.company.finance_api.market.domain.EurobondInstrument;
import com.company.finance_api.market.domain.EurobondQuote;
import com.company.finance_api.market.infrastructure.http.dto.eurobond.EurobondCashflowResponse;
import com.company.finance_api.market.infrastructure.http.dto.eurobond.EurobondHistoryPointDto;
import com.company.finance_api.market.infrastructure.http.dto.eurobond.EurobondHistoryResponse;
import com.company.finance_api.market.infrastructure.http.dto.eurobond.EurobondInstrumentDto;
import com.company.finance_api.market.infrastructure.persistence.EurobondHistoryRepository;
import com.company.finance_api.market.infrastructure.persistence.EurobondInstrumentRepository;
import com.company.finance_api.market.infrastructure.persistence.EurobondQuoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** TrEurobondMarketService iş mantığını uygular (tr eurobond market service). */
@Service
public class TrEurobondMarketService {

  private static final Pattern ISIN_PATTERN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");
  private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365.25");

  private final EurobondInstrumentRepository instrumentRepository;
  private final EurobondQuoteRepository quoteRepository;
  private final EurobondHistoryRepository historyRepository;

  public TrEurobondMarketService(
      EurobondInstrumentRepository instrumentRepository,
      EurobondQuoteRepository quoteRepository,
      EurobondHistoryRepository historyRepository) {
    this.instrumentRepository = instrumentRepository;
    this.quoteRepository = quoteRepository;
    this.historyRepository = historyRepository;
  }

  @Transactional(readOnly = true)
  /** listActiveInstruments işlemini gerçekleştirir. */
  public List<EurobondInstrumentDto> listActiveInstruments() {
    LocalDate today = LocalDate.now();
    List<EurobondInstrument> instruments =
        instrumentRepository.findAllByActiveIsTrueOrderByMaturityDateAsc();
    if (instruments.isEmpty()) {
      return List.of();
    }
    Map<String, EurobondQuote> latestQuotesByIsin = loadLatestQuotesByIsin(instruments);
    List<EurobondInstrumentDto> out = new ArrayList<>(instruments.size());
    for (EurobondInstrument inst : instruments) {
      out.add(toDto(inst, latestQuotesByIsin.get(inst.getIsin()), today));
    }
    return out;
  }

  @Transactional(readOnly = true)
  /** Instrument sorgusunu döner. */
  public EurobondInstrumentDto getInstrument(String rawIsin) {
    String isin = normalizeIsin(rawIsin);
    EurobondInstrument inst =
        instrumentRepository
            .findById(isin)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ISIN"));
    if (!inst.isActive()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument inactive");
    }
    EurobondQuote quote = quoteRepository.findFirstByIsinOrderByQuoteTimeDesc(isin).orElse(null);
    return toDto(inst, quote, LocalDate.now());
  }

  @Transactional(readOnly = true)
  /** History sorgusunu döner. */
  public EurobondHistoryResponse getHistory(String rawIsin, String range, String frequency) {
    String isin = normalizeIsin(rawIsin);
    if (!instrumentRepository.existsById(isin)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ISIN");
    }
    HistoryRange r = HistoryRange.parse(range);
    HistoryFrequency f = HistoryFrequency.parse(frequency);
    LocalDate from = r.fromDate(LocalDate.now());
    List<EurobondHistory> rows =
        historyRepository.findByIsinAndHistoryDateGreaterThanEqualOrderByHistoryDateAsc(isin, from);
    List<EurobondHistoryPointDto> points = aggregate(rows, f);
    return new EurobondHistoryResponse(isin, r.code(), f.name(), points);
  }

  @Transactional(readOnly = true)
  /** cashflow işlemini gerçekleştirir. */
  public EurobondCashflowResponse cashflow(String rawIsin, BigDecimal nominal) {
    if (nominal == null || nominal.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nominal must be positive");
    }
    String isin = normalizeIsin(rawIsin);
    EurobondInstrument inst =
        instrumentRepository
            .findById(isin)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ISIN"));
    EurobondQuote quote = quoteRepository.findFirstByIsinOrderByQuoteTimeDesc(isin).orElse(null);
    BigDecimal clean = quote != null ? quote.getCleanPrice() : null;
    if (clean == null || clean.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "No clean price for cashflow");
    }
    BigDecimal couponPct = inst.getCouponPercent();
    BigDecimal annual =
        nominal.multiply(couponPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal semi = annual.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    BigDecimal approxPurchase =
        nominal.multiply(clean).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    String disclaimer =
        "Bu hesaplama temiz fiyat üzerinden yaklaşık hesaplamadır; birikmiş faiz, vergi ve işlem masrafları dahil değildir.";
    return new EurobondCashflowResponse(
        isin,
        nominal.setScale(2, RoundingMode.HALF_UP),
        couponPct,
        annual,
        semi,
        approxPurchase,
        nominal.setScale(2, RoundingMode.HALF_UP),
        disclaimer);
  }

  private static String normalizeIsin(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISIN required");
    }
    String isin = raw.trim().toUpperCase(Locale.ROOT);
    if (!ISIN_PATTERN.matcher(isin).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ISIN");
    }
    return isin;
  }

  private Map<String, EurobondQuote> loadLatestQuotesByIsin(List<EurobondInstrument> instruments) {
    List<String> isins = instruments.stream().map(EurobondInstrument::getIsin).toList();
    return quoteRepository.findLatestByIsinIn(isins).stream()
        .collect(
            java.util.stream.Collectors.toMap(
                EurobondQuote::getIsin,
                java.util.function.Function.identity(),
                (left, right) ->
                    Comparator.comparing(EurobondQuote::getQuoteTime)
                        .compare(left, right)
                        >= 0
                        ? left
                        : right,
                LinkedHashMap::new));
  }

  private static EurobondInstrumentDto toDto(
      EurobondInstrument inst, EurobondQuote quote, LocalDate today) {
    String displayName = "TR USD Eurobond " + inst.getMaturityDate().getYear();
    BigDecimal remaining = remainingYears(inst.getMaturityDate(), today);
    String src = quote != null ? quote.getSourceProvider() : inst.getSourceProvider();
    return new EurobondInstrumentDto(
        displayName,
        inst.getIsin(),
        inst.getSymbol(),
        inst.getName(),
        inst.getIssuer(),
        inst.getCurrency(),
        inst.getMaturityDate(),
        remaining,
        inst.getCouponPercent(),
        inst.getCouponFrequency(),
        quote != null ? quote.getCleanPrice() : null,
        quote != null ? quote.getBidPrice() : null,
        quote != null ? quote.getAskPrice() : null,
        quote != null ? quote.getYieldToMaturityPercent() : null,
        quote != null ? quote.getDailyChangePercent() : null,
        src,
        quote != null ? quote.getQuoteTime() : null);
  }

  private static BigDecimal remainingYears(LocalDate maturity, LocalDate today) {
    if (!maturity.isAfter(today)) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    }
    long days = ChronoUnit.DAYS.between(today, maturity);
    return BigDecimal.valueOf(days).divide(DAYS_PER_YEAR, 1, RoundingMode.HALF_UP);
  }

  private static List<EurobondHistoryPointDto> aggregate(
      List<EurobondHistory> rows, HistoryFrequency frequency) {
    if (rows.isEmpty()) {
      return List.of();
    }
    if (frequency == HistoryFrequency.DAILY) {
      return rows.stream().map(TrEurobondMarketService::toPoint).toList();
    }
    Map<String, EurobondHistory> bucketLast = new LinkedHashMap<>();
    for (EurobondHistory h : rows) {
      final String key;
      if (frequency == HistoryFrequency.WEEKLY) {
        key =
            h.getHistoryDate().get(IsoFields.WEEK_BASED_YEAR)
                + "-W"
                + h.getHistoryDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      } else if (frequency == HistoryFrequency.MONTHLY) {
        key = h.getHistoryDate().getYear() + "-M" + h.getHistoryDate().getMonthValue();
      } else {
        throw new IllegalStateException("aggregate only for WEEKLY/MONTHLY");
      }
      bucketLast.put(key, h);
    }
    return bucketLast.values().stream()
        .sorted(java.util.Comparator.comparing(EurobondHistory::getHistoryDate))
        .map(TrEurobondMarketService::toPoint)
        .toList();
  }

  private static EurobondHistoryPointDto toPoint(EurobondHistory h) {
    return new EurobondHistoryPointDto(
        h.getHistoryDate(),
        h.getClosePrice(),
        h.getOpenPrice(),
        h.getHighPrice(),
        h.getLowPrice(),
        h.getCloseYieldPercent(),
        h.getOpenYieldPercent(),
        h.getHighYieldPercent(),
        h.getLowYieldPercent(),
        h.getChangePercent(),
        h.getSourceProvider());
  }

  private enum HistoryRange {
    ONE_Y("1Y"),
    FIVE_Y("5Y"),
    ALL("ALL");

    private final String code;

    HistoryRange(String code) {
      this.code = code;
    }

    static HistoryRange parse(String raw) {
      if (raw == null || raw.isBlank()) {
        return FIVE_Y;
      }
      String u = raw.trim().toUpperCase(Locale.ROOT);
      return switch (u) {
        case "1Y" -> ONE_Y;
        case "5Y" -> FIVE_Y;
        case "ALL" -> ALL;
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range");
      };
    }

    LocalDate fromDate(LocalDate today) {
      return switch (this) {
        case ONE_Y -> today.minusDays(365);
        case FIVE_Y -> today.minusDays(365 * 5L);
        case ALL -> LocalDate.of(1990, 1, 1);
      };
    }

    String code() {
      return code;
    }
  }

  private enum HistoryFrequency {
    DAILY,
    WEEKLY,
    MONTHLY;

    static HistoryFrequency parse(String raw) {
      if (raw == null || raw.isBlank()) {
        return DAILY;
      }
      String u = raw.trim().toUpperCase(Locale.ROOT);
      return switch (u) {
        case "DAILY" -> DAILY;
        case "WEEKLY" -> WEEKLY;
        case "MONTHLY" -> MONTHLY;
        default ->
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported frequency");
      };
    }
  }
}
