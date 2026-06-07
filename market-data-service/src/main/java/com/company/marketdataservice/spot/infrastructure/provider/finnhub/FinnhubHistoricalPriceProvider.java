package com.company.marketdataservice.spot.infrastructure.provider.finnhub;
import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.company.marketdataservice.history.domain.HistoricalPricePoint;
import com.company.marketdataservice.history.domain.HistoricalPriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "providers.finnhub.enabled", havingValue = "true")
public class FinnhubHistoricalPriceProvider implements HistoricalPriceProvider {

    private static final String SOURCE = "FINNHUB";
    private final FinnhubClient finnhubClient;
    private final FinnhubProperties properties;

    public FinnhubHistoricalPriceProvider(FinnhubClient finnhubClient, FinnhubProperties properties) {
        this.finnhubClient = finnhubClient;
        this.properties = properties;
    }

    /**
     * Finnhub stock/candle REST API'sinden günlük OHLC mumlarını fetch edip tarih aralığına map eder.
         * @param symbol Finnhub tarafından yönetilen hisse sembolü
         * @param startDate aralık başlangıç tarihi (dahil)
         * @param endDate aralık bitiş tarihi (dahil)
         * @return OPEN/HIGH/LOW/MARKET tipindeki historical price point listesi
         */
    @Override
    public List<HistoricalPricePoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!isManagedByFinnhub(normalized)) {
            return List.of();
        }
        long from = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long to = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;
        FinnhubCandleResponse response = finnhubClient.fetchStockCandles(normalized, "D", from, to);
        List<HistoricalPricePoint> points = mapCandles(normalized, response);
        log.info("FINNHUB HIST FETCH symbol={} points={}", normalized, points.size());
        return points;
    }

    private boolean isManagedByFinnhub(String symbol) {
        return ownedSymbols().contains(symbol);
    }

    /**
     * Konfigürasyonda tanımlı ve bu provider tarafından yönetilen sembol kümesini döner.
         */
    public Set<String> ownedSymbols() {
        return properties.getSymbols() == null
                ? Set.of()
                : properties.getSymbols().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static List<HistoricalPricePoint> mapCandles(String symbol, FinnhubCandleResponse response) {
        if (response == null || response.t() == null || response.t().isEmpty()) {
            return List.of();
        }
        if (!"ok".equalsIgnoreCase(response.s())) {
            return List.of();
        }
        List<HistoricalPricePoint> out = new ArrayList<>();
        for (int i = 0; i < response.t().size(); i++) {
            Long epoch = response.t().get(i);
            if (epoch == null) {
                continue;
            }
            Instant observedAt = Instant.ofEpochSecond(epoch);
            addPoint(out, symbol, response.o(), i, "OPEN", observedAt);
            addPoint(out, symbol, response.h(), i, "HIGH", observedAt);
            addPoint(out, symbol, response.l(), i, "LOW", observedAt);
            addPoint(out, symbol, response.c(), i, "MARKET", observedAt);
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
}
