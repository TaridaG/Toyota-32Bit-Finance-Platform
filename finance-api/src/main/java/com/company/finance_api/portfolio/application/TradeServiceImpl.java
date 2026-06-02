package com.company.finance_api.portfolio.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.portfolio.domain.TransactionAcquisitionFx;
import com.company.finance_api.portfolio.domain.enums.PurchaseMode;
import com.company.finance_api.portfolio.domain.enums.TradeInputMode;
import com.company.finance_api.portfolio.domain.enums.TransactionType;
import com.company.finance_api.portfolio.infrastructure.http.dto.AcquisitionFxRatesSnapshot;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionAcquisitionFxRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.pricing.infrastructure.http.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradeExecutionRequest;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradePreviewResponse;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.messaging.event.TransactionExecutedEvent;
import com.company.finance_api.shared.messaging.event.publisher.TransactionEventPublisher;
import com.company.finance_api.portfolio.domain.FxHistoricalAnchor;
import com.company.finance_api.portfolio.domain.InstrumentListingCurrency;
import com.company.finance_api.portfolio.domain.MdsInstrumentSymbolAliases;
import com.company.finance_api.portfolio.domain.TlDepositInstruments;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.pricing.infrastructure.query.TlDepositIndexQueryService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** TradeServiceImpl iş mantığını uygular (trade service). */
@Service
@RequiredArgsConstructor
@Transactional
public class TradeServiceImpl implements TradeService {

  private final InstrumentRepository instrumentRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionAcquisitionFxRepository transactionAcquisitionFxRepository;
  private final InstrumentPriceRepository instrumentPriceRepository;
  private final JdbcTemplate jdbcTemplate;
  private final CurrencyConversionService currencyConversionService;
  private final PortfolioPerformanceSeriesService portfolioPerformanceSeriesService;
  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final TransactionEventPublisher transactionEventPublisher;
  private final TlDepositIndexQueryService tlDepositIndexQueryService;
  private static final List<PriceType> VALUATION_PRICE_TYPES =
      List.of(PriceType.MARKET, PriceType.FX_MID, PriceType.FUND_NAV);

  /** buy işlemini gerçekleştirir. */
  @Override
  public Transaction buy(Long instrumentId, BigDecimal quantity) {
    TradeExecutionRequest request = new TradeExecutionRequest();
    request.setInstrumentId(instrumentId);
    request.setInputMode(TradeInputMode.LOTS);
    request.setLots(quantity);
    request.setInputCurrency("USD");
    request.setPurchaseMode(PurchaseMode.NOW);
    return buy(request);
  }

  /** preview işlemini gerçekleştirir. */
  @Override
  public TradePreviewResponse preview(TradeExecutionRequest request) {
    Instrument instrument =
        instrumentRepository
            .findByIdAndActiveTrue(request.getInstrumentId())
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));
    Computation computation = compute(request, instrument);
    Instant effectiveAcquiredAt =
        request.getPurchaseMode() == PurchaseMode.PAST ? computation.acquiredAt() : null;
    return new TradePreviewResponse(
        instrument.getId(),
        instrument.getSymbol(),
        computation.instrumentCurrency(),
        computation.lots(),
        computation.inputAmount(),
        computation.inputCurrency(),
        computation.unitPriceUsed(),
        computation.fxRateUsed(),
        computation.manualUnitPriceRequired(),
        computation.unitPriceSource(),
        effectiveAcquiredAt,
        computation.pastDateRolledToEarliestData(),
        computation.acquisitionFxRates());
  }

  /** buy işlemini gerçekleştirir. */
  @Override
  public Transaction buy(TradeExecutionRequest request) {

    UUID userId = currentUserResolver.getCurrentUserId();

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    ExternalPortfolio portfolio = resolvePortfolio(user, request.getPortfolioId());

    Instrument instrument =
        instrumentRepository
            .findByIdAndActiveTrue(request.getInstrumentId())
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

    Computation computation = compute(request, instrument);

    BigDecimal totalCost = computation.totalCost();

    Transaction transaction =
        Transaction.buy(
            user,
            instrument,
            portfolio,
            computation.unitPriceUsed(),
            computation.lots(),
            request.getPurchaseMode(),
            computation.acquiredAt(),
            computation.unitPriceUsed(),
            request.getInputMode(),
            computation.inputCurrency(),
            computation.inputAmount(),
            computation.fxRateUsed(),
            request.getPurchaseMode() == PurchaseMode.PAST ? "PAST_BOUGHT" : "NOW_BOUGHT");

    Transaction saved = transactionRepository.save(transaction);

    AcquisitionFxRatesSnapshot snap = computation.acquisitionFxRates();
    transactionAcquisitionFxRepository.save(
        new TransactionAcquisitionFx(
            saved.getId(),
            computation.fxAsOfUsed(),
            snap.usdTry(),
            snap.eurTry(),
            snap.gbpTry(),
            snap.jpyTry(),
            snap.aedTry(),
            snap.eurUsd(),
            snap.gbpUsd(),
            snap.jpyUsd()));

    //  EVENT
    transactionEventPublisher.publish(
        TransactionExecutedEvent.of(
            user.getId(),
            instrument.getId(),
            instrument.getSymbol(),
            saved.getType(),
            saved.getPrice(),
            saved.getQuantity()));

    if (portfolio != null) {
      portfolioPerformanceSeriesService.recomputePortfolioHistory(user.getId(), portfolio.getId());
    }

    return saved;
  }

  /** sell işlemini gerçekleştirir. */
  @Override
  public Transaction sell(Long instrumentId, BigDecimal quantity) {
    TradeExecutionRequest request = new TradeExecutionRequest();
    request.setInstrumentId(instrumentId);
    request.setInputMode(TradeInputMode.LOTS);
    request.setLots(quantity);
    request.setInputCurrency("USD");
    request.setPurchaseMode(PurchaseMode.NOW);
    return sell(request);
  }

  /** sell işlemini gerçekleştirir. */
  @Override
  public Transaction sell(TradeExecutionRequest request) {

    UUID userId = currentUserResolver.getCurrentUserId();

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    ExternalPortfolio portfolio = resolvePortfolio(user, request.getPortfolioId());

    Instrument instrument =
        instrumentRepository
            .findByIdAndActiveTrue(request.getInstrumentId())
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

    Computation computation = compute(request, instrument);
    BigDecimal quantity = computation.lots();

    // 🔥 POSITION CHECK
    BigDecimal netQuantity =
        transactionRepository
            .findByUserAndInstrumentAndExternalPortfolio(user, instrument, portfolio)
            .stream()
            .map(
                tx ->
                    tx.getType() == TransactionType.BUY
                        ? tx.getQuantity()
                        : tx.getQuantity().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (netQuantity.compareTo(quantity) < 0) {
      throw new IllegalStateException("Insufficient position for sell");
    }

    BigDecimal totalGain = computation.totalCost();

    Transaction transaction =
        Transaction.sell(user, instrument, portfolio, computation.unitPriceUsed(), quantity);

    Transaction saved = transactionRepository.save(transaction);

    transactionEventPublisher.publish(
        TransactionExecutedEvent.of(
            user.getId(),
            instrument.getId(),
            instrument.getSymbol(),
            saved.getType(),
            saved.getPrice(),
            saved.getQuantity()));

    if (portfolio != null) {
      portfolioPerformanceSeriesService.recomputePortfolioHistory(user.getId(), portfolio.getId());
    }

    return saved;
  }

  /** PriceCoverage sorgusunu döner. */
  @Override
  public InstrumentPriceCoverageResponse getPriceCoverage(Long instrumentId) {
    Instrument instrument =
        instrumentRepository
            .findById(instrumentId)
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));
    Optional<Instant> first = earliestKnownPriceInstant(instrument);
    Optional<Instant> last = latestKnownPriceInstant(instrument);
    return new InstrumentPriceCoverageResponse(
        instrument.getId(), instrument.getSymbol(), first.orElse(null), last.orElse(null));
  }

  private Optional<Instant> earliestKnownPriceInstant(Instrument instrument) {
    String canonical = instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
    return minInstant(
        minInstant(
            findFirstAvailablePriceDate(instrument),
            findFirstAvailableMdsMarketPriceDate(instrument)),
        findFirstAvailableFxRateDate(canonical));
  }

  private Optional<Instant> latestKnownPriceInstant(Instrument instrument) {
    String canonical = instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
    return maxInstant(
        maxInstant(
            findLastAvailablePriceDate(instrument),
            findLastAvailableMdsMarketPriceDate(instrument)),
        findLastAvailableFxRateDate(canonical));
  }

  private Computation compute(TradeExecutionRequest request, Instrument instrument) {
    if (request.getInputMode() == null) {
      throw new IllegalArgumentException("inputMode is required");
    }
    if (request.getPurchaseMode() == null) {
      throw new IllegalArgumentException("purchaseMode is required");
    }
    String instrumentCurrency = InstrumentListingCurrency.resolve(instrument);
    if (TlDepositInstruments.isTlDeposit(instrument) && request.getInputMode() != TradeInputMode.AMOUNT) {
      throw new IllegalArgumentException("TL deposit supports amount input only");
    }
    String inputCurrency = currencyConversionService.normalizeCurrency(request.getInputCurrency());
    UnitPriceResolution unitPriceResolution =
        resolveUnitPrice(request, instrument, instrumentCurrency);
    BigDecimal unitPrice = unitPriceResolution.unitPrice();
    Instant acquiredAt =
        request.getPurchaseMode() == PurchaseMode.PAST
            ? unitPriceResolution.pastAcquisitionOverride().orElse(request.getAcquiredAt())
            : Instant.now();
    if (request.getPurchaseMode() == PurchaseMode.PAST && acquiredAt == null) {
      throw new IllegalArgumentException("acquiredAt is required for past purchases");
    }
    boolean historicalFx = request.getPurchaseMode() == PurchaseMode.PAST;
    Instant fxAsOfUsed =
        historicalFx ? FxHistoricalAnchor.normalizeEndOfAcquisitionDay(acquiredAt) : Instant.now();
    BigDecimal fxRate =
        historicalFx
            ? currencyConversionService.convertAt(
                fxAsOfUsed, BigDecimal.ONE, inputCurrency, instrumentCurrency)
            : currencyConversionService.convert(BigDecimal.ONE, inputCurrency, instrumentCurrency);
    if (fxRate == null || fxRate.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalStateException(
          "FX rate unavailable for " + inputCurrency + " -> " + instrumentCurrency);
    }
    AcquisitionFxRatesSnapshot acquisitionFxRates =
        currencyConversionService.acquisitionFxHubSnapshot(fxAsOfUsed, historicalFx);
    BigDecimal lots;
    BigDecimal inputAmount;
    if (request.getInputMode() == TradeInputMode.LOTS) {
      if (request.getLots() == null || request.getLots().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("lots must be positive");
      }
      lots = request.getLots().setScale(6, RoundingMode.HALF_UP);
      BigDecimal totalInInstrumentCurrency = unitPrice.multiply(lots);
      inputAmount = totalInInstrumentCurrency.divide(fxRate, 6, RoundingMode.HALF_UP);
    } else {
      if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("amount must be positive");
      }
      inputAmount = request.getAmount().setScale(6, RoundingMode.HALF_UP);
      BigDecimal totalInInstrumentCurrency = inputAmount.multiply(fxRate);
      lots = totalInInstrumentCurrency.divide(unitPrice, 6, RoundingMode.HALF_UP);
    }
    BigDecimal totalCost = unitPrice.multiply(lots).setScale(6, RoundingMode.HALF_UP);
    return new Computation(
        instrumentCurrency,
        inputCurrency,
        unitPrice,
        fxRate,
        lots,
        inputAmount,
        totalCost,
        acquiredAt,
        unitPriceResolution.manualUnitPriceRequired(),
        unitPriceResolution.sourceLabel(),
        unitPriceResolution.pastDateRolledToEarliestData(),
        fxAsOfUsed,
        acquisitionFxRates);
  }

  private UnitPriceResolution resolveUnitPrice(
      TradeExecutionRequest request, Instrument instrument, String instrumentCurrency) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return resolveTlDepositUnitPrice(request, instrument);
    }
    if (request.getPurchaseMode() == PurchaseMode.PAST) {
      Instant acquiredAt = request.getAcquiredAt();
      if (acquiredAt == null) {
        throw new IllegalArgumentException("acquiredAt is required for past purchases");
      }
      if (request.getUnitPrice() != null && request.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
        return new UnitPriceResolution(
            request.getUnitPrice().setScale(6, RoundingMode.HALF_UP),
            false,
            "MANUAL_INPUT",
            Optional.empty(),
            false);
      }
      Optional<BigDecimal> historicalPrice =
          fetchValuationPriceAtOrBefore(
              instrument, FxHistoricalAnchor.normalizeEndOfAcquisitionDay(acquiredAt));
      if (historicalPrice.isEmpty()) {
        historicalPrice =
            fetchHistoricalPriceFromMds(
                instrument, FxHistoricalAnchor.normalizeEndOfAcquisitionDay(acquiredAt));
      }
      if (historicalPrice.isPresent()) {
        return new UnitPriceResolution(
            historicalPrice.get().setScale(6, RoundingMode.HALF_UP),
            false,
            "HISTORICAL_MARKET_DATA",
            Optional.empty(),
            false);
      }
      Optional<Instant> earliestOpt = earliestKnownPriceInstant(instrument);
      if (earliestOpt.isEmpty()) {
        throw new IllegalArgumentException(
            "unitPrice is required: no historical price data for this instrument");
      }
      Instant earliestInstant = earliestOpt.get();
      Instant normalizedEarliest = FxHistoricalAnchor.normalizeEndOfAcquisitionDay(earliestInstant);
      Optional<BigDecimal> rolledPrice =
          fetchValuationPriceAtOrBefore(instrument, normalizedEarliest);
      if (rolledPrice.isEmpty()) {
        rolledPrice = fetchHistoricalPriceFromMds(instrument, normalizedEarliest);
      }
      if (rolledPrice.isPresent()) {
        Instant dayStartUtc =
            earliestInstant
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        return new UnitPriceResolution(
            rolledPrice.get().setScale(6, RoundingMode.HALF_UP),
            false,
            "HISTORICAL_MARKET_DATA_EARLIEST_AVAILABLE",
            Optional.of(dayStartUtc),
            true);
      }
      String firstAvailable =
          DateTimeFormatter.ISO_LOCAL_DATE.format(earliestInstant.atZone(ZoneOffset.UTC));
      throw new IllegalArgumentException(
          "unitPrice is required: historical price unavailable, first available date="
              + firstAvailable);
    }
    BigDecimal valuationPrice = fetchLatestValuationPriceDirect(instrument);
    if ("TRY".equals(instrumentCurrency) && valuationPrice != null) {
      // MARKET rows for BIST / TRY-native symbols are stored in TRY (MDS publishes Yahoo .IS spot
      // as-is).
      // Do not treat them as USD and multiply by USDTRY — that inflates unit prices (~30–40×).
      return new UnitPriceResolution(
          valuationPrice.setScale(6, RoundingMode.HALF_UP),
          false,
          "LIVE_MARKET_DATA",
          Optional.empty(),
          false);
    }
    return new UnitPriceResolution(
        valuationPrice.setScale(6, RoundingMode.HALF_UP),
        false,
        "LIVE_MARKET_DATA",
        Optional.empty(),
        false);
  }

  private BigDecimal fetchLatestValuationPriceDirect(Instrument instrument) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService
          .getLatestPrice(instrument)
          .map(InstrumentPrice::getPrice)
          .orElseThrow(() -> new IllegalStateException("Price not available"));
    }
    for (PriceType priceType : VALUATION_PRICE_TYPES) {
      Optional<BigDecimal> found =
          instrumentPriceRepository
              .findTopByInstrumentAndPriceTypeOrderByTimestampDesc(instrument, priceType)
              .map(row -> row.getPrice());
      if (found.isPresent() && found.get().compareTo(BigDecimal.ZERO) > 0) {
        return found.get();
      }
    }
    for (String lookup : MdsInstrumentSymbolAliases.historyLookupSymbols(instrument)) {
      Optional<BigDecimal> fromMds = queryLatestMdsMarketPrice(lookup);
      if (fromMds.isPresent() && fromMds.get().compareTo(BigDecimal.ZERO) > 0) {
        return fromMds.get();
      }
    }
    throw new IllegalStateException("Price not available");
  }

  private Optional<BigDecimal> queryLatestMdsMarketPrice(String symbol) {
    String sym = symbol.trim().toUpperCase(Locale.ROOT);
    String sql =
        """
                SELECT price
                FROM public.mds_market_price_history
                WHERE instrument_symbol = ?
                  AND price_type IN ('MARKET', 'FX_MID', 'FUND_NAV')
                ORDER BY observed_at DESC, id DESC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql, rs -> rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty(), sym);
  }

  private Optional<BigDecimal> fetchValuationPriceAtOrBefore(
      Instrument instrument, Instant target) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService
          .getLatestPriceBefore(instrument, target)
          .map(InstrumentPrice::getPrice);
    }
    List<String> symbols = MdsInstrumentSymbolAliases.historyLookupSymbols(instrument);
    if (symbols.isEmpty()) {
      return Optional.empty();
    }
    for (PriceType priceType : VALUATION_PRICE_TYPES) {
      Optional<BigDecimal> found =
          instrumentPriceRepository
              .findLatestPricesAtOrBefore(symbols, priceType.name(), target)
              .stream()
              .findFirst()
              .map(row -> row.getPrice());
      if (found.isPresent() && found.get().compareTo(BigDecimal.ZERO) > 0) {
        return found;
      }
    }
    return Optional.empty();
  }

  private Optional<BigDecimal> fetchHistoricalPriceFromMds(Instrument instrument, Instant target) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService
          .getLatestPriceBefore(instrument, target)
          .map(InstrumentPrice::getPrice);
    }
    for (String sym : MdsInstrumentSymbolAliases.historyLookupSymbols(instrument)) {
      Optional<BigDecimal> fromMarket = queryMdsMarketPriceHistory(sym, target);
      if (fromMarket.isPresent()) {
        return fromMarket;
      }
    }
    String canonical = instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
    return queryMdsFxMidHistory(canonical, target);
  }

  private Optional<Instant> findFirstAvailableMdsMarketPriceDate(Instrument instrument) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService.getFirstAvailableInstant(instrument);
    }
    Optional<Instant> best = Optional.empty();
    for (String sym : MdsInstrumentSymbolAliases.historyLookupSymbols(instrument)) {
      best = minInstant(best, queryMdsFirstMarketObserved(sym));
    }
    return best;
  }

  private Optional<Instant> findLastAvailableMdsMarketPriceDate(Instrument instrument) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService.getLastAvailableInstant(instrument);
    }
    Optional<Instant> best = Optional.empty();
    for (String sym : MdsInstrumentSymbolAliases.historyLookupSymbols(instrument)) {
      best = maxInstant(best, queryMdsLastMarketObserved(sym));
    }
    return best;
  }

  private Optional<Instant> queryMdsFirstMarketObserved(String symbol) {
    String sql =
        """
                SELECT observed_at
                FROM public.mds_market_price_history
                WHERE instrument_symbol = ?
                  AND price_type IN ('MARKET', 'FX_MID', 'FUND_NAV')
                ORDER BY observed_at ASC, id ASC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql,
        rs -> rs.next() ? Optional.of(rs.getTimestamp(1).toInstant()) : Optional.empty(),
        symbol.trim().toUpperCase(Locale.ROOT));
  }

  private Optional<Instant> queryMdsLastMarketObserved(String symbol) {
    String sql =
        """
                SELECT observed_at
                FROM public.mds_market_price_history
                WHERE instrument_symbol = ?
                  AND price_type IN ('MARKET', 'FX_MID', 'FUND_NAV')
                ORDER BY observed_at DESC, id DESC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql,
        rs -> rs.next() ? Optional.of(rs.getTimestamp(1).toInstant()) : Optional.empty(),
        symbol.trim().toUpperCase(Locale.ROOT));
  }

  private Optional<BigDecimal> queryMdsMarketPriceHistory(String symbol, Instant target) {
    String sql =
        """
                SELECT price
                FROM public.mds_market_price_history
                WHERE instrument_symbol = ?
                  AND price_type IN ('MARKET', 'FX_MID', 'FUND_NAV')
                  AND observed_at <= CAST(? AS TIMESTAMPTZ)
                ORDER BY observed_at DESC, id DESC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql,
        rs -> rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty(),
        symbol,
        Timestamp.from(target));
  }

  /**
   * TCMB / composite FX snapshots are persisted to {@code mds_fx_rate_history} (mid), not {@code
   * mds_market_price_history}.
   */
  private Optional<BigDecimal> queryMdsFxMidHistory(String canonicalSymbol, Instant target) {
    String sql =
        """
                SELECT mid AS price
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

  private Optional<Instant> findFirstAvailableFxRateDate(String canonicalSymbol) {
    String sql =
        """
                SELECT observed_at
                FROM public.mds_fx_rate_history
                WHERE canonical_symbol = ?
                ORDER BY observed_at ASC, id ASC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql,
        rs -> rs.next() ? Optional.of(rs.getTimestamp(1).toInstant()) : Optional.empty(),
        canonicalSymbol.trim().toUpperCase(Locale.ROOT));
  }

  private Optional<Instant> findLastAvailableFxRateDate(String canonicalSymbol) {
    String sql =
        """
                SELECT observed_at
                FROM public.mds_fx_rate_history
                WHERE canonical_symbol = ?
                ORDER BY observed_at DESC, id DESC
                LIMIT 1
                """;
    return jdbcTemplate.query(
        sql,
        rs -> rs.next() ? Optional.of(rs.getTimestamp(1).toInstant()) : Optional.empty(),
        canonicalSymbol.trim().toUpperCase(Locale.ROOT));
  }

  private static Optional<Instant> minInstant(Optional<Instant> a, Optional<Instant> b) {
    if (a.isEmpty()) {
      return b;
    }
    if (b.isEmpty()) {
      return a;
    }
    return Optional.of(a.get().isBefore(b.get()) ? a.get() : b.get());
  }

  private static Optional<Instant> maxInstant(Optional<Instant> a, Optional<Instant> b) {
    if (a.isEmpty()) {
      return b;
    }
    if (b.isEmpty()) {
      return a;
    }
    return Optional.of(a.get().isAfter(b.get()) ? a.get() : b.get());
  }

  private Optional<Instant> findFirstAvailablePriceDate(Instrument instrument) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService.getFirstAvailableInstant(instrument);
    }
    for (PriceType priceType : VALUATION_PRICE_TYPES) {
      Optional<Instant> found =
          instrumentPriceRepository
              .findTopByInstrumentAndPriceTypeOrderByTimestampAsc(instrument, priceType)
              .map(row -> row.getTimestamp());
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty();
  }

  private Optional<Instant> findLastAvailablePriceDate(Instrument instrument) {
    if (TlDepositInstruments.isTlDeposit(instrument)) {
      return tlDepositIndexQueryService.getLastAvailableInstant(instrument);
    }
    for (PriceType priceType : VALUATION_PRICE_TYPES) {
      Optional<Instant> found =
          instrumentPriceRepository
              .findTopByInstrumentAndPriceTypeOrderByTimestampDesc(instrument, priceType)
              .map(row -> row.getTimestamp());
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty();
  }

  private ExternalPortfolio resolvePortfolio(User user, Long portfolioId) {
    if (portfolioId == null) {
      return null;
    }
    return externalPortfolioRepository
        .findByIdAndUserId(portfolioId, user.getId())
        .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
  }

  private UnitPriceResolution resolveTlDepositUnitPrice(
      TradeExecutionRequest request, Instrument instrument) {
    if (request.getPurchaseMode() == PurchaseMode.PAST) {
      Instant acquiredAt = request.getAcquiredAt();
      if (acquiredAt == null) {
        throw new IllegalArgumentException("acquiredAt is required for past purchases");
      }
      Optional<BigDecimal> historicalPrice =
          tlDepositIndexQueryService
              .getLatestPriceBefore(
                  instrument, FxHistoricalAnchor.normalizeEndOfAcquisitionDay(acquiredAt))
              .map(InstrumentPrice::getPrice);
      if (historicalPrice.isPresent()) {
        return new UnitPriceResolution(
            historicalPrice.get().setScale(6, RoundingMode.HALF_UP),
            false,
            "TL_DEPOSIT_INDEX_HISTORICAL",
            Optional.empty(),
            false);
      }
      Optional<Instant> earliestOpt = tlDepositIndexQueryService.getFirstAvailableInstant(instrument);
      if (earliestOpt.isEmpty()) {
        throw new IllegalArgumentException("TL deposit historical index unavailable");
      }
      Instant earliestInstant = earliestOpt.get();
      Optional<BigDecimal> rolledPrice =
          tlDepositIndexQueryService
              .getLatestPriceBefore(
                  instrument, FxHistoricalAnchor.normalizeEndOfAcquisitionDay(earliestInstant))
              .map(InstrumentPrice::getPrice);
      if (rolledPrice.isPresent()) {
        Instant dayStartUtc =
            earliestInstant
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        return new UnitPriceResolution(
            rolledPrice.get().setScale(6, RoundingMode.HALF_UP),
            false,
            "TL_DEPOSIT_INDEX_EARLIEST_AVAILABLE",
            Optional.of(dayStartUtc),
            true);
      }
      throw new IllegalArgumentException("TL deposit historical index unavailable");
    }
    BigDecimal valuationPrice =
        tlDepositIndexQueryService
            .getLatestPrice(instrument)
            .map(InstrumentPrice::getPrice)
            .orElseThrow(() -> new IllegalStateException("Price not available"));
    return new UnitPriceResolution(
        valuationPrice.setScale(6, RoundingMode.HALF_UP),
        false,
        "TL_DEPOSIT_INDEX_LIVE",
        Optional.empty(),
        false);
  }

  private record Computation(
      String instrumentCurrency,
      String inputCurrency,
      BigDecimal unitPriceUsed,
      BigDecimal fxRateUsed,
      BigDecimal lots,
      BigDecimal inputAmount,
      BigDecimal totalCost,
      Instant acquiredAt,
      boolean manualUnitPriceRequired,
      String unitPriceSource,
      boolean pastDateRolledToEarliestData,
      Instant fxAsOfUsed,
      AcquisitionFxRatesSnapshot acquisitionFxRates) {}

  private record UnitPriceResolution(
      BigDecimal unitPrice,
      boolean manualUnitPriceRequired,
      String sourceLabel,
      Optional<Instant> pastAcquisitionOverride,
      boolean pastDateRolledToEarliestData) {}
}
