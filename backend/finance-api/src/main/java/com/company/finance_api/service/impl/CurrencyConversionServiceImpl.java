package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.CurrencyConversionService;
import com.company.finance_api.service.PriceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private static final String USD = "USD";
    private static final Set<String> SUPPORTED = Set.of("USD", "EUR", "TRY", "GBP", "JPY");
    private static final Set<String> REQUIRED_RATE_SYMBOLS = Set.of("USDTRY", "EURUSD", "GBPUSD", "JPYUSD");
    private static final Duration RATE_CACHE_TTL = Duration.ofSeconds(5);

    private final InstrumentRepository instrumentRepository;
    private final PriceService priceService;

    private final Map<String, BigDecimal> cachedRates = new ConcurrentHashMap<>();
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    public CurrencyConversionServiceImpl(
            InstrumentRepository instrumentRepository,
            PriceService priceService
    ) {
        this.instrumentRepository = instrumentRepository;
        this.priceService = priceService;
    }

    @Override
    public BigDecimal convert(BigDecimal price, String from, String to) {
        if (price == null) {
            return null;
        }
        String source = normalizeCurrency(from);
        String target = normalizeCurrency(to);
        if (source.equals(target)) {
            return price;
        }

        Map<String, BigDecimal> rates = getRatesSnapshot();
        BigDecimal amountInUsd = toUsd(price, source, rates);
        if (amountInUsd == null) {
            return price;
        }

        BigDecimal converted = fromUsd(amountInUsd, target, rates);
        if (converted == null) {
            return price;
        }
        return converted;
    }

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
            for (String symbol : REQUIRED_RATE_SYMBOLS) {
                resolveLatestFxRate(symbol).ifPresent(rate -> {
                    if (rate.compareTo(BigDecimal.ZERO) > 0) {
                        next.put(symbol, rate);
                    }
                });
            }
            cachedRates.clear();
            cachedRates.putAll(next);
            cacheExpiresAt = Instant.now().plus(RATE_CACHE_TTL);
            return cachedRates;
        }
    }

    private Optional<BigDecimal> resolveLatestFxRate(String symbol) {
        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<InstrumentPrice> latest = priceService.getLatestValuationPrice(instrumentOpt.get());
        if (latest.isEmpty() || latest.get().getPrice() == null) {
            return Optional.empty();
        }
        return Optional.of(latest.get().getPrice());
    }

    private BigDecimal toUsd(BigDecimal amount, String from, Map<String, BigDecimal> rates) {
        return switch (from) {
            case "USD" -> amount;
            case "TRY" -> divide(amount, rates.get("USDTRY"));
            case "EUR" -> multiply(amount, rates.get("EURUSD"));
            case "GBP" -> multiply(amount, rates.get("GBPUSD"));
            case "JPY" -> multiply(amount, rates.get("JPYUSD"));
            default -> null;
        };
    }

    private BigDecimal fromUsd(BigDecimal usdAmount, String to, Map<String, BigDecimal> rates) {
        return switch (to) {
            case "USD" -> usdAmount;
            case "TRY" -> multiply(usdAmount, rates.get("USDTRY"));
            case "EUR" -> divide(usdAmount, rates.get("EURUSD"));
            case "GBP" -> divide(usdAmount, rates.get("GBPUSD"));
            case "JPY" -> divide(usdAmount, rates.get("JPYUSD"));
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
