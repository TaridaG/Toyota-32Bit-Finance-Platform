package com.company.marketdataservice.spot.infrastructure.provider.yahoo;
import com.company.marketdataservice.spot.infrastructure.provider.PriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component("yahooStock")
@Slf4j
public class YahooFinanceProvider implements PriceProvider {

    private static final String SOURCE = "YAHOO";
    private final YahooFinanceClient yahooFinanceClient;

    public YahooFinanceProvider(YahooFinanceClient yahooFinanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String source() {
        return SOURCE;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @return işlem sonucu
         */
    @Override
    public BigDecimal fetchPrice(String symbol) {
        return fetchSpotQuote(symbol).price();
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @return işlem sonucu
         */
    public YahooSpotQuote fetchSpotQuote(String symbol) {
        log.info("YAHOO FETCH START symbol={}", symbol);
        String normalized = normalizeSymbol(symbol);
        List<String> candidates = toCandidateSymbols(normalized);
        IllegalStateException lastServerSide = null;
        for (String candidate : candidates) {
            try {
                YahooFinanceResponse response = yahooFinanceClient.fetchSpotChart(candidate);
                YahooSpotQuote quote = mapSpotQuote(response, normalized);
                if (quote.price() != null) {
                    log.info(
                            "YAHOO FETCH RESULT symbol={} price={} timestamp={}",
                            quote.symbol(),
                            quote.price(),
                            quote.timestamp()
                    );
                    return quote;
                }
            } catch (IllegalArgumentException ex) {
                // 4xx: unsupported symbol candidate. continue to next candidate
            } catch (IllegalStateException ex) {
                lastServerSide = ex;
            }
        }
        if (lastServerSide != null) {
            throw lastServerSide;
        }
        throw new IllegalArgumentException("Yahoo did not return price for symbol=" + normalized);
    }

    private static YahooSpotQuote mapSpotQuote(YahooFinanceResponse response, String fallbackSymbol) {
        YahooFinanceResponse.Result result = firstResult(response);
        if (result == null || result.meta() == null || result.meta().regularMarketPrice() == null) {
            throw new IllegalArgumentException("Yahoo spot payload has no market price for symbol=" + fallbackSymbol);
        }
        YahooFinanceResponse.Meta meta = result.meta();
        String symbol = meta.symbol() == null ? fallbackSymbol : meta.symbol();
        BigDecimal price = BigDecimal.valueOf(meta.regularMarketPrice());
        Instant ts = meta.regularMarketTime() == null
                ? Instant.now()
                : Instant.ofEpochSecond(meta.regularMarketTime());
        return new YahooSpotQuote(
                symbol,
                price,
                ts,
                SOURCE,
                toBigDecimal(meta.regularMarketVolume()),
                toBigDecimal(meta.openInterest()),
                toBigDecimal(meta.regularMarketOpen()),
                toBigDecimal(meta.regularMarketDayHigh()),
                toBigDecimal(meta.regularMarketDayLow()),
                firstNonBlank(meta.exchangeName(), meta.fullExchangeName()),
                meta.underlyingSymbol(),
                meta.expireDate() == null || meta.expireDate() <= 0
                        ? null
                        : Instant.ofEpochSecond(meta.expireDate()));
    }

    private static BigDecimal toBigDecimal(Double value) {
        if (value == null || !Double.isFinite(value) || value <= 0d) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private static BigDecimal toBigDecimal(Long value) {
        if (value == null || value <= 0L) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static YahooFinanceResponse.Result firstResult(YahooFinanceResponse response) {
        if (response == null || response.chart() == null || response.chart().result() == null || response.chart().result().isEmpty()) {
            return null;
        }
        return response.chart().result().get(0);
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> toCandidateSymbols(String symbol) {
        if (symbol.contains(".")) {
            return List.of(symbol);
        }
        if (isLikelyCrypto(symbol) || symbol.endsWith("TRY")) {
            return List.of(symbol);
        }
        // Try raw symbol first, then BIST-style suffix. This keeps US symbols working
        // while allowing Turkish equities like GARAN/THYAO/ASELS to resolve as *.IS.
        return List.of(symbol, symbol + ".IS");
    }

    private static boolean isLikelyCrypto(String symbol) {
        return symbol.endsWith("USDT") || symbol.endsWith("USD");
    }

}
