package com.company.finance_api.service.impl;

import com.company.finance_api.domain.*;
import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TradeInputMode;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.dto.TradeExecutionRequest;
import com.company.finance_api.dto.TradePreviewResponse;
import com.company.finance_api.event.TransactionExecutedEvent;
import com.company.finance_api.event.publisher.TransactionEventPublisher;
import com.company.finance_api.portfolio.InstrumentListingCurrency;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.*;
import com.company.finance_api.service.CurrencyConversionService;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeServiceImpl implements TradeService {

    private final InstrumentRepository instrumentRepository;
    private final TransactionRepository transactionRepository;
    private final InstrumentPriceRepository instrumentPriceRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CurrencyConversionService currencyConversionService;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;
    private final ExternalPortfolioRepository externalPortfolioRepository;
    private final TransactionEventPublisher transactionEventPublisher;
    private static final List<com.company.finance_api.domain.enums.PriceType> VALUATION_PRICE_TYPES = List.of(
            com.company.finance_api.domain.enums.PriceType.MARKET,
            com.company.finance_api.domain.enums.PriceType.FX_MID,
            com.company.finance_api.domain.enums.PriceType.FUND_NAV
    );

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

    @Override
    public TradePreviewResponse preview(TradeExecutionRequest request) {
        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));
        Computation computation = compute(request, instrument);
        return new TradePreviewResponse(
                instrument.getId(),
                instrument.getSymbol(),
                computation.instrumentCurrency,
                computation.lots,
                computation.inputAmount,
                computation.inputCurrency,
                computation.unitPriceUsed,
                computation.fxRateUsed,
                computation.manualUnitPriceRequired,
                computation.unitPriceSource
        );
    }

    @Override
    public Transaction buy(TradeExecutionRequest request) {

        UUID userId = currentUserResolver.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        ExternalPortfolio portfolio = resolvePortfolio(user, request.getPortfolioId());


        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

        Computation computation = compute(request, instrument);

        BigDecimal totalCost = computation.totalCost;

        Transaction transaction = Transaction.buy(
                user,
                instrument,
                portfolio,
                computation.unitPriceUsed,
                computation.lots,
                request.getPurchaseMode(),
                computation.acquiredAt,
                computation.unitPriceUsed,
                request.getInputMode(),
                computation.inputCurrency,
                computation.inputAmount,
                computation.fxRateUsed,
                request.getPurchaseMode() == PurchaseMode.PAST ? "PAST_BOUGHT" : "NOW_BOUGHT"
        );

        Transaction saved = transactionRepository.save(transaction);

//  EVENT
        transactionEventPublisher.publish(
                TransactionExecutedEvent.of(
                        user.getId(),
                        instrument.getId(),
                        instrument.getSymbol(),
                        saved.getType(),
                        saved.getPrice(),
                        saved.getQuantity()
                )
        );

        return saved;
    }

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

    @Override
    public Transaction sell(TradeExecutionRequest request) {

        UUID userId = currentUserResolver.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        ExternalPortfolio portfolio = resolvePortfolio(user, request.getPortfolioId());

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

        Computation computation = compute(request, instrument);
        BigDecimal quantity = computation.lots;

        // 🔥 POSITION CHECK
        BigDecimal netQuantity = transactionRepository
                .findByUserAndInstrumentAndExternalPortfolio(user, instrument, portfolio)
                .stream()
                .map(tx -> tx.getType() == TransactionType.BUY
                        ? tx.getQuantity()
                        : tx.getQuantity().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (netQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient position for sell");
        }

        BigDecimal totalGain = computation.totalCost;

        Transaction transaction = Transaction.sell(
                user,
                instrument,
                portfolio,
                computation.unitPriceUsed,
                quantity
        );

        Transaction saved = transactionRepository.save(transaction);

        transactionEventPublisher.publish(
                TransactionExecutedEvent.of(
                        user.getId(),
                        instrument.getId(),
                        instrument.getSymbol(),
                        saved.getType(),
                        saved.getPrice(),
                        saved.getQuantity()
                )
        );

        return saved;
    }

    @Override
    public InstrumentPriceCoverageResponse getPriceCoverage(Long instrumentId) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));
        Optional<Instant> first = earliestKnownPriceInstant(instrument);
        Optional<Instant> last = latestKnownPriceInstant(instrument);
        return new InstrumentPriceCoverageResponse(
                instrument.getId(),
                instrument.getSymbol(),
                first.orElse(null),
                last.orElse(null)
        );
    }

    private Optional<Instant> earliestKnownPriceInstant(Instrument instrument) {
        String canonical = instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
        return minInstant(findFirstAvailablePriceDate(instrument), findFirstAvailableFxRateDate(canonical));
    }

    private Optional<Instant> latestKnownPriceInstant(Instrument instrument) {
        String canonical = instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
        return maxInstant(findLastAvailablePriceDate(instrument), findLastAvailableFxRateDate(canonical));
    }

    private Computation compute(TradeExecutionRequest request, Instrument instrument) {
        if (request.getInputMode() == null) {
            throw new IllegalArgumentException("inputMode is required");
        }
        if (request.getPurchaseMode() == null) {
            throw new IllegalArgumentException("purchaseMode is required");
        }
        String instrumentCurrency = InstrumentListingCurrency.resolve(instrument);
        String inputCurrency = currencyConversionService.normalizeCurrency(request.getInputCurrency());
        UnitPriceResolution unitPriceResolution = resolveUnitPrice(request, instrument, instrumentCurrency);
        BigDecimal unitPrice = unitPriceResolution.unitPrice();
        BigDecimal fxRate = currencyConversionService.convert(BigDecimal.ONE, inputCurrency, instrumentCurrency);
        if (fxRate == null || fxRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("FX rate unavailable for " + inputCurrency + " -> " + instrumentCurrency);
        }
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
        Instant acquiredAt = request.getPurchaseMode() == PurchaseMode.PAST
                ? request.getAcquiredAt()
                : Instant.now();
        if (request.getPurchaseMode() == PurchaseMode.PAST && acquiredAt == null) {
            throw new IllegalArgumentException("acquiredAt is required for past purchases");
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
                unitPriceResolution.sourceLabel()
        );
    }

    private UnitPriceResolution resolveUnitPrice(TradeExecutionRequest request, Instrument instrument, String instrumentCurrency) {
        if (request.getPurchaseMode() == PurchaseMode.PAST) {
            Instant acquiredAt = request.getAcquiredAt();
            if (acquiredAt == null) {
                throw new IllegalArgumentException("acquiredAt is required for past purchases");
            }
            if (request.getUnitPrice() != null && request.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                return new UnitPriceResolution(
                        request.getUnitPrice().setScale(6, RoundingMode.HALF_UP),
                        false,
                        "MANUAL_INPUT"
                );
            }
            Optional<BigDecimal> historicalPrice = fetchValuationPriceAtOrBefore(instrument, normalizeHistoricalTarget(acquiredAt));
            if (historicalPrice.isEmpty()) {
                historicalPrice = fetchHistoricalPriceFromMds(instrument.getSymbol(), normalizeHistoricalTarget(acquiredAt));
            }
            if (historicalPrice.isPresent()) {
                return new UnitPriceResolution(
                        historicalPrice.get().setScale(6, RoundingMode.HALF_UP),
                        false,
                        "HISTORICAL_MARKET_DATA"
                );
            }
            String firstAvailable = earliestKnownPriceInstant(instrument)
                    .map(value -> DateTimeFormatter.ISO_LOCAL_DATE.format(value.atZone(ZoneOffset.UTC)))
                    .orElse("unknown");
            throw new IllegalArgumentException(
                    "unitPrice is required: historical price unavailable for selected date, first available date=" + firstAvailable
            );
        }
        BigDecimal valuationPrice = fetchLatestValuationPriceDirect(instrument);
        if ("TRY".equals(instrumentCurrency) && valuationPrice != null) {
            // MARKET rows for BIST / TRY-native symbols are stored in TRY (MDS publishes Yahoo .IS spot as-is).
            // Do not treat them as USD and multiply by USDTRY — that inflates unit prices (~30–40×).
            return new UnitPriceResolution(valuationPrice.setScale(6, RoundingMode.HALF_UP), false, "LIVE_MARKET_DATA");
        }
        return new UnitPriceResolution(valuationPrice.setScale(6, RoundingMode.HALF_UP), false, "LIVE_MARKET_DATA");
    }

    private BigDecimal fetchLatestValuationPriceDirect(Instrument instrument) {
        for (com.company.finance_api.domain.enums.PriceType priceType : VALUATION_PRICE_TYPES) {
            Optional<BigDecimal> found = instrumentPriceRepository
                    .findTopByInstrumentAndPriceTypeOrderByTimestampDesc(instrument, priceType)
                    .map(row -> row.getPrice());
            if (found.isPresent() && found.get().compareTo(BigDecimal.ZERO) > 0) {
                return found.get();
            }
        }
        throw new IllegalStateException("Price not available");
    }

    private Optional<BigDecimal> fetchValuationPriceAtOrBefore(Instrument instrument, Instant target) {
        for (com.company.finance_api.domain.enums.PriceType priceType : VALUATION_PRICE_TYPES) {
            Optional<BigDecimal> found = instrumentPriceRepository
                    .findLatestPricesAtOrBefore(List.of(instrument.getSymbol()), priceType.name(), target)
                    .stream()
                    .findFirst()
                    .map(row -> row.getPrice());
            if (found.isPresent() && found.get().compareTo(BigDecimal.ZERO) > 0) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> fetchHistoricalPriceFromMds(String symbol, Instant target) {
        String sym = symbol.trim().toUpperCase(Locale.ROOT);
        Optional<BigDecimal> fromMarket = queryMdsMarketPriceHistory(sym, target);
        if (fromMarket.isPresent()) {
            return fromMarket;
        }
        return queryMdsFxMidHistory(sym, target);
    }

    private Optional<BigDecimal> queryMdsMarketPriceHistory(String symbol, Instant target) {
        String sql = """
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
                Timestamp.from(target)
        );
    }

    /**
     * TCMB / composite FX snapshots are persisted to {@code mds_fx_rate_history} (mid), not {@code mds_market_price_history}.
     */
    private Optional<BigDecimal> queryMdsFxMidHistory(String canonicalSymbol, Instant target) {
        String sql = """
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
                Timestamp.from(target)
        );
    }

    private Optional<Instant> findFirstAvailableFxRateDate(String canonicalSymbol) {
        String sql = """
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
        String sql = """
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
        for (com.company.finance_api.domain.enums.PriceType priceType : VALUATION_PRICE_TYPES) {
            Optional<Instant> found = instrumentPriceRepository
                    .findTopByInstrumentAndPriceTypeOrderByTimestampAsc(instrument, priceType)
                    .map(row -> row.getTimestamp());
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<Instant> findLastAvailablePriceDate(Instrument instrument) {
        for (com.company.finance_api.domain.enums.PriceType priceType : VALUATION_PRICE_TYPES) {
            Optional<Instant> found = instrumentPriceRepository
                    .findTopByInstrumentAndPriceTypeOrderByTimestampDesc(instrument, priceType)
                    .map(row -> row.getTimestamp());
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /**
     * Date picker requests commonly arrive as midnight UTC for a day.
     * In that case search until end-of-day so we can use any price from that day.
     */
    private Instant normalizeHistoricalTarget(Instant acquiredAt) {
        ZonedDateTime utc = acquiredAt.atZone(ZoneOffset.UTC);
        if (utc.getHour() == 0 && utc.getMinute() == 0 && utc.getSecond() == 0 && utc.getNano() == 0) {
            return utc.plusDays(1).minusNanos(1).toInstant();
        }
        return acquiredAt;
    }

    private ExternalPortfolio resolvePortfolio(User user, Long portfolioId) {
        if (portfolioId == null) {
            return null;
        }
        return externalPortfolioRepository.findByIdAndUserId(portfolioId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
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
            String unitPriceSource
    ) {}

    private record UnitPriceResolution(
            BigDecimal unitPrice,
            boolean manualUnitPriceRequired,
            String sourceLabel
    ) {}

}