package com.company.marketdataservice.provider.yahoo;

import com.company.marketdataservice.historical.HistoricalPricePoint;
import com.company.marketdataservice.historical.HistoricalPriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class YahooFinanceHistoricalPriceProvider implements HistoricalPriceProvider {

    private static final String SOURCE = "YAHOO";
    private final YahooFinanceClient yahooFinanceClient;

    public YahooFinanceHistoricalPriceProvider(YahooFinanceClient yahooFinanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
    }

    @Override
    public List<HistoricalPricePoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (isLikelyCrypto(normalized) || normalized.endsWith("TRY")) {
            return List.of();
        }
        List<String> candidates = normalized.contains(".")
                ? List.of(normalized)
                : List.of(normalized, normalized + ".IS");
        for (String candidate : candidates) {
            try {
                YahooFinanceResponse response = yahooFinanceClient.fetchHistoricalChart(candidate, "5y", "1d");
                List<HistoricalPricePoint> mapped = mapHistorical(response, normalized);
                if (!mapped.isEmpty()) {
                    List<HistoricalPricePoint> points = mapped.stream()
                            .filter(p -> {
                                LocalDate day = p.occurredAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                                return !day.isBefore(startDate) && !day.isAfter(endDate);
                            })
                            .toList();
                    log.info("YAHOO HIST FETCH symbol={} points={}", normalized, points.size());
                    return points;
                }
            } catch (IllegalArgumentException ex) {
                // 4xx -> try next candidate
            }
        }
        return List.of();
    }

    private static List<HistoricalPricePoint> mapHistorical(YahooFinanceResponse response, String symbol) {
        if (response == null || response.chart() == null || response.chart().result() == null || response.chart().result().isEmpty()) {
            return List.of();
        }
        YahooFinanceResponse.Result result = response.chart().result().get(0);
        if (result.timestamp() == null || result.timestamp().isEmpty() || result.indicators() == null
                || result.indicators().quote() == null || result.indicators().quote().isEmpty()) {
            return List.of();
        }
        YahooFinanceResponse.Quote quote = result.indicators().quote().get(0);
        List<HistoricalPricePoint> out = new ArrayList<>();
        for (int i = 0; i < result.timestamp().size(); i++) {
            Long epoch = result.timestamp().get(i);
            if (epoch == null) {
                continue;
            }
            Instant observedAt = Instant.ofEpochSecond(epoch);
            addPoint(out, symbol, quote.open(), i, "OPEN", observedAt);
            addPoint(out, symbol, quote.high(), i, "HIGH", observedAt);
            addPoint(out, symbol, quote.low(), i, "LOW", observedAt);
            addPoint(out, symbol, quote.close(), i, "MARKET", observedAt);
        }
        return out;
    }

    private static void addPoint(
            List<HistoricalPricePoint> out,
            String symbol,
            List<Double> values,
            int index,
            String type,
            Instant observedAt
    ) {
        if (values == null || index >= values.size()) {
            return;
        }
        Double v = values.get(index);
        if (v == null || !Double.isFinite(v)) {
            return;
        }
        out.add(new HistoricalPricePoint(
                symbol,
                BigDecimal.valueOf(v),
                type,
                SOURCE,
                observedAt,
                null
        ));
    }

    private static boolean isLikelyCrypto(String symbol) {
        return symbol.endsWith("USDT") || symbol.endsWith("USD");
    }
}
