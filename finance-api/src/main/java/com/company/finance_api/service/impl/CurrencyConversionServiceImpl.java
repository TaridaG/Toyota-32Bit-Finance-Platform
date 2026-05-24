package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.dto.AcquisitionFxRatesSnapshot;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.CurrencyConversionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * FX conversion for portfolio and market views.
 *
 * <p><b>Primary model (TRY hub):</b> TCMB-style instruments {@code USDTRY}, {@code EURTRY}, {@code
 * GBPTRY}, {@code JPYTRY}, {@code AEDTRY} quote <strong>TRY per 1 unit of the base
 * currency</strong> (same convention as {@code USDTRY} in this codebase). Any supported display
 * currency is reached as {@code amount → TRY → target}, so the UI’s {@code X-Currency} choice uses
 * the same cross-rates everywhere.
 *
 * <p><b>Fallback:</b> If the TRY bridge is incomplete, the legacy USD triangle ({@code EURUSD},
 * {@code GBPUSD}, {@code JPYUSD}, {@code USDTRY}) is used. If both fail, {@link #convert} returns
 * {@code null} (callers must not treat foreign-currency magnitudes as already converted).
 *
 * <p><b>JPY conventions:</b> TCMB often publishes {@code JPYTRY} as <strong>TRY per 100
 * JPY</strong>; we normalize to TRY per 1 JPY (any raw {@code > 1} is treated as the per-100
 * scale). {@code JPYUSD} may appear as <strong>JPY per 1 USD</strong> (large number); the hub
 * expects <strong>USD per 1 JPY</strong> and inverts when {@code > 10}. Some feeds also store
 * <strong>USD per 100 JPY</strong> (~0.66); values between {@code 0.03} and {@code 1} are re-scaled
 * by {@code ÷100}.
 */
@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

  private static final Logger log = LoggerFactory.getLogger(CurrencyConversionServiceImpl.class);

  private static final String USD = "USD";

  /** Must match selectable UI currencies (see {@code frontend-web} preferences). */
  private static final Set<String> SUPPORTED = Set.of("USD", "EUR", "TRY", "GBP", "JPY", "AED");

  /**
   * All symbols we attempt to load. TRY crosses are preferred; USD pairs back-fill when a TRY quote
   * is missing.
   */
  private static final List<String> RATE_SYMBOL_LOAD_ORDER =
      List.of("USDTRY", "EURTRY", "GBPTRY", "JPYTRY", "AEDTRY", "EURUSD", "GBPUSD", "JPYUSD");

  private static final Duration RATE_CACHE_TTL = Duration.ofSeconds(5);
  private static final List<PriceType> RATE_PRICE_TYPES =
      List.of(PriceType.FX_MID, PriceType.MARKET, PriceType.FUND_NAV);

  /**
   * TCMB / EVDS often ship {@code JPYTRY} as TRY per 100 JPY; the TRY hub expects TRY per 1 JPY.
   * {@code JPYUSD} may be JPY per USD (invert) or USD per 100 JPY (÷100); chain paths expect USD
   * per 1 JPY.
   */
  static BigDecimal normalizeFxMidForTryHub(String symbol, BigDecimal raw) {
    if (raw == null || raw.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    if ("JPYTRY".equals(symbol)) {
      // TCMB / EVDS: TRY per 100 JPY is typically ~15–40; TRY per 1 JPY is ~0.15–0.45. Never > 1
      // for real JPY.
      if (raw.compareTo(BigDecimal.ONE) > 0) {
        return raw.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP);
      }
      return raw;
    }
    if ("JPYUSD".equals(symbol)) {
      BigDecimal r = raw;
      if (r.compareTo(BigDecimal.TEN) > 0) {
        r = BigDecimal.ONE.divide(r, 12, RoundingMode.HALF_UP);
      }
      // USD per 100 JPY mis-stored as USD per 1 JPY (~0.66 vs ~0.0066).
      if (r.compareTo(new BigDecimal("0.03")) > 0 && r.compareTo(BigDecimal.ONE) < 0) {
        r = r.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP);
      }
      return r;
    }
    return raw;
  }

  private final InstrumentRepository instrumentRepository;
  private final InstrumentPriceRepository instrumentPriceRepository;
  private final JdbcTemplate jdbcTemplate;

  private final Map<String, BigDecimal> cachedRates = new ConcurrentHashMap<>();
  private volatile Instant cacheExpiresAt = Instant.EPOCH;

  public CurrencyConversionServiceImpl(
      InstrumentRepository instrumentRepository,
      InstrumentPriceRepository instrumentPriceRepository,
      JdbcTemplate jdbcTemplate) {
    this.instrumentRepository = instrumentRepository;
    this.instrumentPriceRepository = instrumentPriceRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** convert işlemini gerçekleştirir. */

  /** convert işlemini gerçekleştirir. */
  @Override
  public BigDecimal convert(BigDecimal price, String from, String to) {
    return convertUsingHub(getRatesSnapshot(), price, from, to);
  }

  /** convertAt işlemini gerçekleştirir. */
  @Override
  public BigDecimal convertAt(
      Instant fxAsOfEndExclusive, BigDecimal price, String from, String to) {
    return convertUsingHub(loadMdsFxHubAt(fxAsOfEndExclusive), price, from, to);
  }

  /** acquisitionFxHubSnapshot işlemini gerçekleştirir. */
  @Override
  public AcquisitionFxRatesSnapshot acquisitionFxHubSnapshot(
      Instant fxAsOfEndExclusive, boolean historical) {
    Map<String, BigDecimal> rates =
        historical ? loadMdsFxHubAt(fxAsOfEndExclusive) : getRatesSnapshot();
    return new AcquisitionFxRatesSnapshot(
        fxAsOfEndExclusive.toString(),
        rates.get("USDTRY"),
        rates.get("EURTRY"),
        rates.get("GBPTRY"),
        rates.get("JPYTRY"),
        rates.get("AEDTRY"),
        rates.get("EURUSD"),
        rates.get("GBPUSD"),
        rates.get("JPYUSD"));
  }

  private BigDecimal convertUsingHub(
      Map<String, BigDecimal> rates, BigDecimal price, String from, String to) {
    if (price == null) {
      return null;
    }
    String source = normalizeCurrency(from);
    String target = normalizeCurrency(to);
    if (source.equals(target)) {
      return price;
    }

    BigDecimal viaTry = convertThroughTryHub(price, source, target, rates);
    if (viaTry != null) {
      return viaTry;
    }

    BigDecimal amountInUsd = toUsdLegacy(price, source, rates);
    if (amountInUsd == null) {
      log.debug("FX_CONVERT_FAIL no_try_hub no_usd_leg from={} to={}", source, target);
      return null;
    }
    BigDecimal legacy = fromUsdLegacy(amountInUsd, target, rates);
    if (legacy == null) {
      log.debug("FX_CONVERT_FAIL legacy_to_null from={} to={}", source, target);
    }
    return legacy;
  }

  /** normalizeCurrency işlemini gerçekleştirir. */
  @Override
  public String normalizeCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return USD;
    }
    String normalized = currency.trim().toUpperCase(Locale.ROOT);
    if (!SUPPORTED.contains(normalized)) {
      return USD;
    }
    return normalized;
  }

  /** Rate sorgusunu döner. */
  @Override
  public Optional<BigDecimal> getRate(String symbol) {
    return Optional.ofNullable(getRatesSnapshot().get(symbol));
  }

  private Map<String, BigDecimal> getRatesSnapshot() {
    Instant now = Instant.now();
    if (now.isBefore(cacheExpiresAt) && !cachedRates.isEmpty()) {
      return cachedRates;
    }
    synchronized (this) {
      now = Instant.now();
      if (now.isBefore(cacheExpiresAt) && !cachedRates.isEmpty()) {
        return cachedRates;
      }
      Map<String, BigDecimal> next = new ConcurrentHashMap<>();
      for (String symbol : RATE_SYMBOL_LOAD_ORDER) {
        resolveLatestFxRate(symbol)
            .ifPresent(
                rate -> {
                  BigDecimal n = normalizeFxMidForTryHub(symbol, rate);
                  if (n != null && n.compareTo(BigDecimal.ZERO) > 0) {
                    next.put(symbol, n);
                  }
                });
      }
      cachedRates.clear();
      cachedRates.putAll(next);
      cacheExpiresAt = Instant.now().plus(RATE_CACHE_TTL);
      return cachedRates;
    }
  }

  private Map<String, BigDecimal> loadMdsFxHubAt(Instant fxAsOfEndExclusive) {
    Map<String, BigDecimal> next = new HashMap<>();
    for (String symbol : RATE_SYMBOL_LOAD_ORDER) {
      queryMdsFxMidAtOrBefore(symbol, fxAsOfEndExclusive)
          .ifPresent(
              rate -> {
                BigDecimal n = normalizeFxMidForTryHub(symbol, rate);
                if (n != null && n.compareTo(BigDecimal.ZERO) > 0) {
                  next.put(symbol, n);
                }
              });
    }
    return next;
  }

  private Optional<BigDecimal> queryMdsFxMidAtOrBefore(String canonicalSymbol, Instant target) {
    String sql =
        """
                SELECT mid
                FROM public.mds_fx_rate_history
                WHERE canonical_symbol = ?
                  AND observed_at <= CAST(? AS TIMESTAMPTZ)
                ORDER BY observed_at DESC, id DESC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql,
        rs -> rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty(),
        canonicalSymbol,
        Timestamp.from(target));
  }

  private Optional<BigDecimal> resolveLatestFxRate(String symbol) {
    Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
    if (instrumentOpt.isEmpty()) {
      return Optional.empty();
    }
    for (PriceType priceType : RATE_PRICE_TYPES) {
      Optional<BigDecimal> rate =
          instrumentPriceRepository
              .findTopByInstrumentAndPriceTypeOrderByTimestampDesc(instrumentOpt.get(), priceType)
              .map(row -> row.getPrice());
      if (rate.isPresent() && rate.get().compareTo(BigDecimal.ZERO) > 0) {
        return rate;
      }
    }
    return Optional.empty();
  }

  /** Converts using TRY as the intermediate numeraire (TRY per 1 unit of each fiat). */
  private BigDecimal convertThroughTryHub(
      BigDecimal amount, String from, String to, Map<String, BigDecimal> rates) {
    BigDecimal inTry = toTryAmount(amount, from, rates);
    if (inTry == null) {
      return null;
    }
    return fromTryAmount(inTry, to, rates);
  }

  private BigDecimal toTryAmount(BigDecimal amount, String from, Map<String, BigDecimal> rates) {
    return switch (from) {
      case "TRY" -> amount;
      case "USD" -> multiply(amount, rates.get("USDTRY"));
      case "EUR" ->
          firstNonNull(
              multiply(amount, rates.get("EURTRY")),
              multiplyChain(amount, rates.get("EURUSD"), rates.get("USDTRY")));
      case "GBP" ->
          firstNonNull(
              multiply(amount, rates.get("GBPTRY")),
              multiplyChain(amount, rates.get("GBPUSD"), rates.get("USDTRY")));
      case "JPY" ->
          firstNonNull(
              multiply(amount, rates.get("JPYTRY")),
              multiplyChain(amount, rates.get("JPYUSD"), rates.get("USDTRY")));
      case "AED" -> multiply(amount, rates.get("AEDTRY"));
      default -> null;
    };
  }

  private BigDecimal fromTryAmount(BigDecimal tryAmount, String to, Map<String, BigDecimal> rates) {
    return switch (to) {
      case "TRY" -> tryAmount;
      case "USD" -> divide(tryAmount, rates.get("USDTRY"));
      case "EUR" ->
          firstNonNull(
              divide(tryAmount, rates.get("EURTRY")),
              divide(divide(tryAmount, rates.get("USDTRY")), rates.get("EURUSD")));
      case "GBP" ->
          firstNonNull(
              divide(tryAmount, rates.get("GBPTRY")),
              divide(divide(tryAmount, rates.get("USDTRY")), rates.get("GBPUSD")));
      case "JPY" ->
          firstNonNull(
              divide(tryAmount, rates.get("JPYTRY")),
              divide(divide(tryAmount, rates.get("USDTRY")), rates.get("JPYUSD")));
      case "AED" -> divide(tryAmount, rates.get("AEDTRY"));
      default -> null;
    };
  }

  private static BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
    return a != null ? a : b;
  }

  private BigDecimal multiplyChain(BigDecimal amount, BigDecimal first, BigDecimal second) {
    if (amount == null) {
      return null;
    }
    return multiply(multiply(amount, first), second);
  }

  /** Legacy USD-centered paths (kept as fallback). */
  private BigDecimal toUsdLegacy(BigDecimal amount, String from, Map<String, BigDecimal> rates) {
    return switch (from) {
      case "USD" -> amount;
      case "TRY" -> divide(amount, rates.get("USDTRY"));
      case "EUR" -> multiply(amount, rates.get("EURUSD"));
      case "GBP" -> {
        BigDecimal viaUsd = multiply(amount, rates.get("GBPUSD"));
        if (viaUsd != null) {
          yield viaUsd;
        }
        BigDecimal tryLeg = multiply(amount, rates.get("GBPTRY"));
        yield divide(tryLeg, rates.get("USDTRY"));
      }
      case "JPY" -> multiply(amount, rates.get("JPYUSD"));
      case "AED" -> {
        BigDecimal tryA = multiply(amount, rates.get("AEDTRY"));
        yield divide(tryA, rates.get("USDTRY"));
      }
      default -> null;
    };
  }

  private BigDecimal fromUsdLegacy(BigDecimal usdAmount, String to, Map<String, BigDecimal> rates) {
    return switch (to) {
      case "USD" -> usdAmount;
      case "TRY" -> multiply(usdAmount, rates.get("USDTRY"));
      case "EUR" -> divide(usdAmount, rates.get("EURUSD"));
      case "GBP" -> {
        BigDecimal viaUsd = divide(usdAmount, rates.get("GBPUSD"));
        if (viaUsd != null) {
          yield viaUsd;
        }
        BigDecimal tryCross = multiply(usdAmount, rates.get("USDTRY"));
        yield divide(tryCross, rates.get("GBPTRY"));
      }
      case "JPY" -> divide(usdAmount, rates.get("JPYUSD"));
      case "AED" -> {
        BigDecimal tryCross = multiply(usdAmount, rates.get("USDTRY"));
        yield divide(tryCross, rates.get("AEDTRY"));
      }
      default -> null;
    };
  }

  private BigDecimal multiply(BigDecimal left, BigDecimal right) {
    if (left == null || right == null || right.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return left.multiply(right);
  }

  private BigDecimal divide(BigDecimal left, BigDecimal right) {
    if (left == null || right == null || right.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return left.divide(right, 12, RoundingMode.HALF_UP);
  }
}
