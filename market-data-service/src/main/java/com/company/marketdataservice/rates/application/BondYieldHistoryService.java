package com.company.marketdataservice.rates.application;

import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import com.company.marketdataservice.catalog.registry.providers.BondRegistry;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.rates.infrastructure.http.dto.BondYieldCurvePointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.BondYieldCurveResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.BondYieldLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TCMB Hazine tahvil/bono ikincil piyasa getiri serileri (TRBOND*) için güncel ve geçmiş sorgular.
 */
@Service
public class BondYieldHistoryService {

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");
    private static final String DEFAULT_TENOR = "5Y";
    private static final List<String> TENOR_ORDER = List.of("1Y", "2Y", "3Y", "5Y", "10Y");

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final TcmbBondMarketProperties bondProperties;

    public BondYieldHistoryService(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            TcmbBondMarketProperties bondProperties
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.bondProperties = bondProperties;
    }

    public BondYieldLatestDto loadLatest(String tenorParam) {
        ResolvedTenor resolved = resolveTenor(tenorParam);
        return buildLatest(resolved);
    }

    public PolicyRateHistoryResponseDto loadFiveYearDailyFromDb(String tenorParam) {
        ResolvedTenor resolved = resolveTenor(tenorParam);
        PolicyRateHistoryResponseDto out = new PolicyRateHistoryResponseDto();
        out.setSymbol(resolved.symbol());
        out.setName("TR Government Bond " + resolved.tenor());
        out.setFrequency("DAILY");
        out.setSourceProvider("TCMB_EVDS");

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5);
        Instant fromInclusive = rangeStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = rangeEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<HistoryPointDto> rows =
                marketPriceHistoryRepository.findHistoryPoints(resolved.symbol(), fromInclusive, toExclusive);
        if (rows == null || rows.isEmpty()) {
            out.setPoints(List.of());
            return out;
        }

        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        for (HistoryPointDto row : rows) {
            if (row.time() == null || row.value() == null) {
                continue;
            }
            LocalDate day = row.time().atZone(ZoneOffset.UTC).toLocalDate();
            byDay.put(day, row.value().setScale(2, RoundingMode.HALF_UP));
        }

        List<PolicyRateHistoryPointDto> points = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PolicyRateHistoryPointDto(e.getKey(), e.getValue()))
                .toList();
        out.setPoints(points);
        return out;
    }

    public BondYieldCurveResponseDto loadCurveLatest() {
        BondYieldCurveResponseDto out = new BondYieldCurveResponseDto();
        Map<String, ResolvedTenor> bySymbol = configuredTenors().stream()
                .collect(Collectors.toMap(ResolvedTenor::symbol, t -> t, (a, b) -> a, LinkedHashMap::new));

        List<MarketPriceHistoryRepository.LatestMarketPriceView> latestRows =
                marketPriceHistoryRepository.findLatestTrbondPricesPerSymbol();
        if (latestRows == null || latestRows.isEmpty()) {
            out.setPoints(List.of());
            return out;
        }

        LocalDate maxObs = null;
        List<BondYieldCurvePointDto> points = new ArrayList<>();
        for (String tenor : TENOR_ORDER) {
            ResolvedTenor resolved = bySymbol.values().stream()
                    .filter(t -> t.tenor().equals(tenor))
                    .findFirst()
                    .orElse(null);
            if (resolved == null) {
                continue;
            }
            MarketPriceHistoryRepository.LatestMarketPriceView row = latestRows.stream()
                    .filter(r -> resolved.symbol().equals(r.getSymbol()))
                    .findFirst()
                    .orElse(null);
            if (row == null || row.getPrice() == null) {
                continue;
            }
            LocalDate obs = row.getTimestamp() == null
                    ? null
                    : row.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
            if (obs != null && (maxObs == null || obs.isAfter(maxObs))) {
                maxObs = obs;
            }
            Integer bp = computeChange1dBasisPoints(resolved.symbol());
            points.add(new BondYieldCurvePointDto(
                    resolved.tenor(),
                    resolved.symbol(),
                    row.getPrice().setScale(2, RoundingMode.HALF_UP),
                    bp
            ));
        }
        out.setObservationDate(maxObs);
        out.setPoints(points);
        return out;
    }

    private BondYieldLatestDto buildLatest(ResolvedTenor resolved) {
        BondYieldLatestDto dto = new BondYieldLatestDto();
        dto.setTenor(resolved.tenor());
        dto.setSymbol(resolved.symbol());
        dto.setEvdsSeries(resolved.evdsSeries());

        List<MarketPriceHistoryRepository.DailyCloseView> closes =
                marketPriceHistoryRepository.findLastTwoDailyCloses(resolved.symbol());
        if (closes == null || closes.isEmpty() || closes.get(0).getPrice() == null) {
            return dto;
        }

        MarketPriceHistoryRepository.DailyCloseView cur = closes.get(0);
        dto.setValue(cur.getPrice().setScale(2, RoundingMode.HALF_UP));
        LocalDate todayTr = LocalDate.now(TR);
        LocalDate observation = cur.getDay();
        if (observation != null && observation.isAfter(todayTr)) {
            observation = todayTr;
        }
        dto.setObservationDate(observation);

        if (closes.size() >= 2 && closes.get(1).getPrice() != null) {
            BigDecimal diff = cur.getPrice().subtract(closes.get(1).getPrice());
            dto.setChange1dBasisPoints(
                    diff.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue()
            );
        }
        return dto;
    }

    private Integer computeChange1dBasisPoints(String symbol) {
        List<MarketPriceHistoryRepository.DailyCloseView> closes =
                marketPriceHistoryRepository.findLastTwoDailyCloses(symbol);
        if (closes == null || closes.size() < 2) {
            return null;
        }
        BigDecimal cur = closes.get(0).getPrice();
        BigDecimal prior = closes.get(1).getPrice();
        if (cur == null || prior == null) {
            return null;
        }
        BigDecimal diff = cur.subtract(prior);
        return diff.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private ResolvedTenor resolveTenor(String raw) {
        Set<String> allowed = configuredTenors().stream().map(ResolvedTenor::tenor).collect(Collectors.toSet());
        if (!StringUtils.hasText(raw)) {
            if (allowed.contains(DEFAULT_TENOR)) {
                return configuredTenors().stream()
                        .filter(t -> DEFAULT_TENOR.equals(t.tenor()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No bond tenors configured"));
            }
            return configuredTenors().get(0);
        }
        String tenor = raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(tenor)) {
            throw new IllegalArgumentException("Unsupported tenor (allowed: " + allowed + ")");
        }
        return configuredTenors().stream()
                .filter(t -> tenor.equals(t.tenor()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported tenor (allowed: " + allowed + ")"));
    }

    private List<ResolvedTenor> configuredTenors() {
        List<ResolvedTenor> out = new ArrayList<>();
        for (TcmbBondMarketProperties.TcmbBondSeries row : bondProperties.getTracked()) {
            if (row == null || !StringUtils.hasText(row.getSymbol())) {
                continue;
            }
            String symbol = row.getSymbol().trim().toUpperCase(Locale.ROOT);
            String tenor = symbolToTenor(symbol).orElse(null);
            if (tenor == null) {
                continue;
            }
            out.add(new ResolvedTenor(tenor, symbol, row.getEvdsSeries()));
        }
        if (out.isEmpty()) {
            for (BondRegistry.BondIngestRow row : BondRegistry.ingestRows()) {
                String tenor = symbolToTenor(row.symbol()).orElse(null);
                if (tenor != null) {
                    out.add(new ResolvedTenor(tenor, row.symbol(), row.evdsSeries()));
                }
            }
        }
        out.sort(Comparator.comparingInt(t -> TENOR_ORDER.indexOf(t.tenor())));
        return List.copyOf(out);
    }

    private Optional<String> symbolToTenor(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return Optional.empty();
        }
        String up = symbol.trim().toUpperCase(Locale.ROOT);
        if (!up.startsWith("TRBOND") || !up.endsWith("Y")) {
            return Optional.empty();
        }
        String mid = up.substring("TRBOND".length(), up.length() - 1);
        if (mid.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mid + "Y");
    }

    private record ResolvedTenor(String tenor, String symbol, String evdsSeries) {}
}
