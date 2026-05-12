package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.dto.PortfolioOverviewItemResponse;
import com.company.finance_api.dto.PortfolioOverviewResponse;
import com.company.finance_api.portfolio.InstrumentListingCurrency;
import com.company.finance_api.portfolio.PositionCostBasisCalculator;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.CurrencyConversionService;
import com.company.finance_api.service.PortfolioOverviewService;
import com.company.finance_api.service.PriceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PortfolioOverviewServiceImpl implements PortfolioOverviewService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioOverviewServiceImpl.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(5);
    private static final String USD = "USD";

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;
    private final PositionCostBasisCalculator positionCostBasisCalculator;
    private final PriceService priceService;
    private final CurrencyConversionService currencyConversionService;
    private final ExternalPortfolioRepository externalPortfolioRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;

    public PortfolioOverviewServiceImpl(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            CurrentUserResolver currentUserResolver,
            PositionCostBasisCalculator positionCostBasisCalculator,
            PriceService priceService,
            CurrencyConversionService currencyConversionService,
            ExternalPortfolioRepository externalPortfolioRepository,
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.currentUserResolver = currentUserResolver;
        this.positionCostBasisCalculator = positionCostBasisCalculator;
        this.priceService = priceService;
        this.currencyConversionService = currencyConversionService;
        this.externalPortfolioRepository = externalPortfolioRepository;
        this.objectMapper = objectMapper;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    @Override
    public PortfolioOverviewResponse getMyOverview(String targetCurrency, Long portfolioId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        String normalizedCurrency = currencyConversionService.normalizeCurrency(targetCurrency);
        String cacheKey = cacheKey(userId, normalizedCurrency, portfolioId);
        Optional<PortfolioOverviewResponse> cached = readFromCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        List<Transaction> transactions;
        if (portfolioId != null) {
            var portfolio = externalPortfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);
            if (portfolio == null) {
                return emptyOverview(normalizedCurrency);
            }
            transactions = transactionRepository.findByUserAndExternalPortfolioOrderByCreatedAtDesc(user, portfolio);
        } else {
            transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        }
        Map<Instrument, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getInstrument));

        Instant startOfTodayUtc = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<PortfolioOverviewItemResponse> items = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalYesterdayValue = BigDecimal.ZERO;

        for (Map.Entry<Instrument, List<Transaction>> entry : grouped.entrySet()) {
            Instrument instrument = entry.getKey();
            List<Transaction> instrumentTx = entry.getValue();
            PositionCostBasisCalculator.PositionCostBasis basis = positionCostBasisCalculator.calculate(instrumentTx);
            if (basis.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            PositionCostBasisCalculator.PositionCostBasis basisBeforeToday =
                    positionCostBasisCalculator.calculateHoldingsBefore(instrumentTx, startOfTodayUtc);
            BigDecimal quantityMarkPriorDay = basisBeforeToday.quantity();

            BigDecimal avgBuyPrice = convertAndScale(basis.averageCost(), instrument, normalizedCurrency);
            BigDecimal positionCost = convertAndScale(basis.totalCost(), instrument, normalizedCurrency);
            if (avgBuyPrice == null || positionCost == null) {
                log.warn(
                        "PORTFOLIO_OVERVIEW_FX_SKIP symbol={} reason=cost_or_avg_conversion_null target={}",
                        instrument.getSymbol(),
                        normalizedCurrency);
                continue;
            }
            totalCost = totalCost.add(positionCost);

            BigDecimal currentPrice = priceService.getLatestValuationPrice(instrument)
                    .map(price -> convertAndScale(price.getPrice(), instrument, normalizedCurrency))
                    .orElse(null);

            BigDecimal value = null;
            BigDecimal priorDayValue = null;
            BigDecimal pnl = null;
            BigDecimal pnlPercent = null;

            if (currentPrice != null) {
                value = applyScale(currentPrice.multiply(basis.quantity()), instrument.getType());
                pnl = applyScale(value.subtract(positionCost), instrument.getType());
                if (positionCost.compareTo(BigDecimal.ZERO) > 0) {
                    pnlPercent = pnl.multiply(BigDecimal.valueOf(100))
                            .divide(positionCost, 4, RoundingMode.HALF_UP);
                }
                totalValue = totalValue.add(value);

                if (quantityMarkPriorDay.compareTo(BigDecimal.ZERO) > 0) {
                    Optional<BigDecimal> priorConvertedOpt = priceService
                            .getLatestValuationPriceBefore(instrument, startOfTodayUtc)
                            .map(InstrumentPrice::getPrice)
                            .map(p -> convertAndScale(p, instrument, normalizedCurrency));
                    if (priorConvertedOpt.isPresent()) {
                        BigDecimal yesterdayLine = applyScale(
                                priorConvertedOpt.get().multiply(quantityMarkPriorDay),
                                instrument.getType());
                        totalYesterdayValue = totalYesterdayValue.add(yesterdayLine);
                        priorDayValue = yesterdayLine;
                    }
                }
            }

            items.add(new PortfolioOverviewItemResponse(
                    instrument.getId(),
                    instrument.getSymbol(),
                    instrument.getName(),
                    instrument.getType().name(),
                    instrument.getExchange() != null ? instrument.getExchange().name() : null,
                    basis.quantity(),
                    avgBuyPrice,
                    currentPrice,
                    value,
                    priorDayValue,
                    pnl,
                    pnlPercent
            ));
        }

        items.sort(Comparator.comparing(PortfolioOverviewItemResponse::symbol, String.CASE_INSENSITIVE_ORDER));

        BigDecimal totalPnl = totalValue.subtract(totalCost);
        BigDecimal totalPnlPercent = totalCost.compareTo(BigDecimal.ZERO) > 0
                ? totalPnl.multiply(BigDecimal.valueOf(100)).divide(totalCost, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal dayOverDayChange = applyScale(totalValue.subtract(totalYesterdayValue), InstrumentType.STOCK);

        PortfolioOverviewResponse response = new PortfolioOverviewResponse(
                normalizedCurrency,
                applyScale(totalValue, InstrumentType.STOCK),
                applyScale(totalCost, InstrumentType.STOCK),
                applyScale(totalPnl, InstrumentType.STOCK),
                totalPnlPercent,
                dayOverDayChange,
                items
        );
        writeToCache(cacheKey, response);
        return response;
    }

    /**
     * Converts a value that is expressed in the instrument's listing/quote currency
     * (TRY for XAUTRY, USD for US equities, etc.) into the portfolio display currency.
     * Historically this path incorrectly assumed USD for all instruments, which blew up
     * TRY-denominated cost bases when the UI was in TRY.
     */
    private BigDecimal convertAndScale(BigDecimal value, Instrument instrument, String targetCurrency) {
        if (value == null) {
            return null;
        }
        String source = InstrumentListingCurrency.resolve(instrument);
        BigDecimal converted = currencyConversionService.convert(value, source, targetCurrency);
        return applyScale(converted, instrument.getType());
    }

    private BigDecimal applyScale(BigDecimal value, InstrumentType type) {
        if (value == null) {
            return null;
        }
        if (type == InstrumentType.CRYPTO) {
            return value.setScale(6, RoundingMode.HALF_UP);
        }
        if (type == InstrumentType.FX) {
            return value.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<PortfolioOverviewResponse> readFromCache(String key) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return Optional.empty();
            }
            String payload = redis.opsForValue().get(key);
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            PortfolioOverviewResponse value = objectMapper.readValue(payload, new TypeReference<>() {
            });
            return Optional.of(value);
        } catch (Exception ex) {
            log.debug("PORTFOLIO_OVERVIEW_CACHE_READ_FAIL key={} reason={}", key, ex.toString());
            return Optional.empty();
        }
    }

    private void writeToCache(String key, PortfolioOverviewResponse value) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return;
            }
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (Exception ex) {
            log.debug("PORTFOLIO_OVERVIEW_CACHE_WRITE_FAIL key={} reason={}", key, ex.toString());
        }
    }

    private String cacheKey(UUID userId, String currency, Long portfolioId) {
        return "portfolio:overview:v8:user:" + userId + ":currency:" + currency.toUpperCase(Locale.ROOT) + ":portfolio:" + (portfolioId == null ? "all" : portfolioId);
    }

    private PortfolioOverviewResponse emptyOverview(String currency) {
        return new PortfolioOverviewResponse(
                currency,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                List.of()
        );
    }
}
