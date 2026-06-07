package com.company.marketdataservice.spot.infrastructure.provider.binance;
import com.company.marketdataservice.history.domain.HistoricalPricePoint;
import com.company.marketdataservice.history.domain.HistoricalPriceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class BinanceHistoricalPriceProvider implements HistoricalPriceProvider {

    private static final String SOURCE = "BINANCE";

    private final WebClient webClient;

    @Value("${providers.binance.base-url:https://api.binance.com}")
    private String baseUrl;

    public BinanceHistoricalPriceProvider(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @param startDate girdi parametresi
         * @param endDate girdi parametresi
         * @return işlem sonucu
         */
    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricalPricePoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!normalized.endsWith("USDT")) {
            return List.of();
        }

        long startTime = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endTime = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1L;
        String uri = baseUrl + "/api/v3/klines?symbol=" + normalized
                + "&interval=1d&startTime=" + startTime
                + "&endTime=" + endTime
                + "&limit=1000";

        List<?> payload = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        List<HistoricalPricePoint> out = new ArrayList<>();
        for (Object row : payload) {
            if (!(row instanceof List<?> columns) || columns.size() < 7) {
                continue;
            }
            Instant observedAt = toInstant(columns.get(0));
            if (observedAt == null) {
                continue;
            }
            addKlinePoint(out, normalized, columns.get(1), "OPEN", observedAt);
            addKlinePoint(out, normalized, columns.get(2), "HIGH", observedAt);
            addKlinePoint(out, normalized, columns.get(3), "LOW", observedAt);
            addKlinePoint(out, normalized, columns.get(4), "MARKET", observedAt);
        }
        out.sort(Comparator.comparing(HistoricalPricePoint::occurredAt));
        return out;
    }

    private static void addKlinePoint(
            List<HistoricalPricePoint> out,
            String symbol,
            Object rawPrice,
            String priceType,
            Instant observedAt
    ) {
        BigDecimal price = toBigDecimal(rawPrice);
        if (price == null) {
            return;
        }
        out.add(new HistoricalPricePoint(symbol, price, priceType, SOURCE, observedAt, null));
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (raw instanceof String value && !value.isBlank()) {
            return new BigDecimal(value);
        }
        return null;
    }

    private static Instant toInstant(Object raw) {
        if (raw instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (raw instanceof String value && !value.isBlank()) {
            return Instant.ofEpochMilli(Long.parseLong(value));
        }
        return null;
    }
}
