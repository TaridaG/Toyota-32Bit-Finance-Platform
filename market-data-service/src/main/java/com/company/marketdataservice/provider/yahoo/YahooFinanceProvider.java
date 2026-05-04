package com.company.marketdataservice.provider.yahoo;

import com.company.marketdataservice.provider.PriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Component("yahooStock")
@Slf4j
public class YahooFinanceProvider implements PriceProvider {

    private static final String SOURCE = "YAHOO";
    private final YahooFinanceClient yahooFinanceClient;

    public YahooFinanceProvider(YahooFinanceClient yahooFinanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public BigDecimal fetchPrice(String symbol) {
        log.info("YAHOO FETCH START symbol={}", symbol);
        String normalized = normalizeSymbol(symbol);
        List<String> candidates = toCandidateSymbols(normalized);
        IllegalStateException lastServerSide = null;
        for (String candidate : candidates) {
            try {
                YahooFinanceResponse response = yahooFinanceClient.fetchSpotChart(candidate);
                YahooTick tick = mapSpot(response, normalized);
                if (tick.price() != null) {
                    log.info(
                            "YAHOO FETCH RESULT symbol={} price={} timestamp={}",
                            tick.symbol(),
                            tick.price(),
                            tick.timestamp()
                    );
                    return tick.price();
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

    private static YahooTick mapSpot(YahooFinanceResponse response, String fallbackSymbol) {
        YahooFinanceResponse.Result result = firstResult(response);
        if (result == null || result.meta() == null || result.meta().regularMarketPrice() == null) {
            throw new IllegalArgumentException("Yahoo spot payload has no market price for symbol=" + fallbackSymbol);
        }
        String symbol = result.meta().symbol() == null ? fallbackSymbol : result.meta().symbol();
        BigDecimal price = BigDecimal.valueOf(result.meta().regularMarketPrice());
        Instant ts = result.meta().regularMarketTime() == null
                ? Instant.now()
                : Instant.ofEpochSecond(result.meta().regularMarketTime());
        return new YahooTick(symbol, price, ts, SOURCE);
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

    record YahooTick(
            String symbol,
            BigDecimal price,
            Instant timestamp,
            String source
    ) {
    }
}
