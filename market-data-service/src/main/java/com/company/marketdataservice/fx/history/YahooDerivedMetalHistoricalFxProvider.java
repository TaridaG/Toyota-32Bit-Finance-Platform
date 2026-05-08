package com.company.marketdataservice.fx.history;

import com.company.marketdataservice.historical.HistoricalFxPoint;
import com.company.marketdataservice.historical.HistoricalFxProvider;
import com.company.marketdataservice.history.FxRateHistoryRepository;
import com.company.marketdataservice.provider.yahoo.YahooFinanceClient;
import com.company.marketdataservice.provider.yahoo.YahooFinanceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class YahooDerivedMetalHistoricalFxProvider implements HistoricalFxProvider {
    private static final Logger log = LoggerFactory.getLogger(YahooDerivedMetalHistoricalFxProvider.class);

    private static final BigDecimal HALF_SPREAD = new BigDecimal("0.001");

    private static final Map<String, String> YAHOO_FUTURES_BY_CANONICAL = Map.of(
            "XAUTRY", "GC=F",
            "XAGTRY", "SI=F",
            "XPTTRY", "PL=F",
            "XPDTRY", "PA=F",
            "XCUTRY", "HG=F"
    );

    private final YahooFinanceClient yahooFinanceClient;
    private final FxRateHistoryRepository fxRateHistoryRepository;
    private final ObjectMapper objectMapper;
    private final WebClient fxWebClient;

    public YahooDerivedMetalHistoricalFxProvider(
            YahooFinanceClient yahooFinanceClient,
            FxRateHistoryRepository fxRateHistoryRepository,
            ObjectMapper objectMapper,
            @Qualifier("fxWebClient") WebClient fxWebClient
    ) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.fxRateHistoryRepository = fxRateHistoryRepository;
        this.objectMapper = objectMapper;
        this.fxWebClient = fxWebClient;
    }

    @Override
    public List<HistoricalFxPoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        String canonical = symbol.trim().toUpperCase(Locale.ROOT);
        String yahooSymbol = YAHOO_FUTURES_BY_CANONICAL.get(canonical);
        if (yahooSymbol == null) {
            return List.of();
        }

        Map<LocalDate, BigDecimal> usdTryByDate = loadUsdTryByDate(startDate, endDate);
        if (usdTryByDate.isEmpty()) {
            log.info("YAHOO_DERIVED_FX_EMPTY symbol={} reason=usdtry_missing from={} to={}", canonical, startDate, endDate);
            return List.of();
        }
        Map<LocalDate, BigDecimal> metalUsdByDate = loadYahooDailyCloseByDate(yahooSymbol, startDate, endDate);
        if (metalUsdByDate.isEmpty()) {
            log.info("YAHOO_DERIVED_FX_EMPTY symbol={} reason=metal_missing providerSymbol={} from={} to={}", canonical, yahooSymbol, startDate, endDate);
            return List.of();
        }

        String base = canonical.replace("TRY", "");
        List<HistoricalFxPoint> out = new ArrayList<>();
        BigDecimal lastUsdTry = null;
        List<LocalDate> days = new ArrayList<>(metalUsdByDate.keySet());
        days.sort(Comparator.naturalOrder());
        for (LocalDate d : days) {
            BigDecimal usdTry = usdTryByDate.getOrDefault(d, lastUsdTry);
            if (usdTry == null || usdTry.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            lastUsdTry = usdTry;
            BigDecimal metalUsd = metalUsdByDate.get(d);
            if (metalUsd == null || metalUsd.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal mid = metalUsd.multiply(usdTry).setScale(6, RoundingMode.HALF_UP);
            BigDecimal bid = mid.subtract(mid.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal ask = mid.add(mid.multiply(HALF_SPREAD)).setScale(6, RoundingMode.HALF_UP);
            Instant observedAt = d.atStartOfDay().toInstant(ZoneOffset.UTC);
            out.add(new HistoricalFxPoint(
                    canonical,
                    null,
                    base,
                    "TRY",
                    bid,
                    ask,
                    mid,
                    observedAt,
                    "YAHOO_DERIVED_SPOT"
            ));
        }
        return out;
    }

    private Map<LocalDate, BigDecimal> loadYahooDailyCloseByDate(String yahooSymbol, LocalDate startDate, LocalDate endDate) {
        try {
            YahooFinanceResponse response = yahooFinanceClient.fetchHistoricalChart(yahooSymbol, "10y", "1d");
            if (response == null || response.chart() == null || response.chart().result() == null || response.chart().result().isEmpty()) {
                return Map.of();
            }
            YahooFinanceResponse.Result first = response.chart().result().get(0);
            if (first == null || first.timestamp() == null || first.timestamp().isEmpty()
                    || first.indicators() == null || first.indicators().quote() == null || first.indicators().quote().isEmpty()
                    || first.indicators().quote().get(0) == null || first.indicators().quote().get(0).close() == null) {
                return Map.of();
            }
            List<Long> ts = first.timestamp();
            List<Double> close = first.indicators().quote().get(0).close();
            int n = Math.min(ts.size(), close.size());
            Map<LocalDate, BigDecimal> out = new HashMap<>();
            for (int i = 0; i < n; i++) {
                Long tNode = ts.get(i);
                Double cNode = close.get(i);
                if (tNode == null || cNode == null || cNode <= 0d) {
                    continue;
                }
                LocalDate d = Instant.ofEpochSecond(tNode).atZone(ZoneOffset.UTC).toLocalDate();
                if (d.isBefore(startDate) || d.isAfter(endDate)) {
                    continue;
                }
                BigDecimal v = BigDecimal.valueOf(cNode);
                out.put(d, v);
            }
            if (!out.isEmpty()) {
                return out;
            }
            return loadYahooDailyCloseByDateRaw(yahooSymbol, startDate, endDate);
        } catch (Exception ignored) {
            return loadYahooDailyCloseByDateRaw(yahooSymbol, startDate, endDate);
        }
    }

    private Map<LocalDate, BigDecimal> loadYahooDailyCloseByDateRaw(String yahooSymbol, LocalDate startDate, LocalDate endDate) {
        try {
            long period1 = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long period2 = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            String uri = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol
                    + "?period1=" + period1
                    + "&period2=" + period2
                    + "&interval=1d&events=history";
            String body = fxWebClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
            if (body == null || body.isBlank()) {
                return Map.of();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) {
                return Map.of();
            }
            JsonNode first = result.get(0);
            JsonNode ts = first.path("timestamp");
            JsonNode close = first.path("indicators").path("quote").path(0).path("close");
            if (!ts.isArray() || !close.isArray()) {
                return Map.of();
            }
            int n = Math.min(ts.size(), close.size());
            Map<LocalDate, BigDecimal> out = new HashMap<>();
            for (int i = 0; i < n; i++) {
                JsonNode tNode = ts.get(i);
                JsonNode cNode = close.get(i);
                if (tNode == null || !tNode.isNumber() || cNode == null || !cNode.isNumber()) {
                    continue;
                }
                LocalDate d = Instant.ofEpochSecond(tNode.asLong()).atZone(ZoneOffset.UTC).toLocalDate();
                if (d.isBefore(startDate) || d.isAfter(endDate)) {
                    continue;
                }
                BigDecimal v = cNode.decimalValue();
                if (v.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                out.put(d, v);
            }
            return out;
        } catch (Exception ex) {
            log.warn("YAHOO_RAW_HISTORY_FALLBACK_FAILED symbol={} reason={}", yahooSymbol, ex.getMessage());
            return Map.of();
        }
    }

    private Map<LocalDate, BigDecimal> loadUsdTryByDate(LocalDate startDate, LocalDate endDate) {
        Instant fromInclusive = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<LocalDate, BigDecimal> out = new HashMap<>();
        fxRateHistoryRepository.findHistoryPoints("USDTRY", fromInclusive, toExclusive)
                .forEach(p -> {
                    if (p == null || p.time() == null || p.value() == null || p.value().compareTo(BigDecimal.ZERO) <= 0) {
                        return;
                    }
                    LocalDate d = p.time().atZone(ZoneOffset.UTC).toLocalDate();
                    out.put(d, p.value());
                });
        return out;
    }

}
