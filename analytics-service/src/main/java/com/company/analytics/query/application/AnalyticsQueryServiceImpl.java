package com.company.analytics.query.application;

import com.company.analytics.processing.domain.AnalyticsPriceCandle;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.enums.CandleInterval;
import com.company.analytics.processing.infrastructure.persistence.*;
import com.company.analytics.query.infrastructure.http.dto.*;
import com.company.analytics.shared.integration.FinanceInstrumentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/**
 * {@link AnalyticsQueryService} arayüzünün varsayılan implementasyonu.
 * Persistence katmanından okunan domain entity'lerini query DTO'larına dönüştürür.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsPriceCandleDailyRepository candleRepository;
    private final AnalyticsPriceCandleRepository multiIntervalCandleRepository;
    private final AnalyticsMovingAverageRepository movingAverageRepository;
    private final AnalyticsRSIRepository rsiRepository;
    private final AnalyticsTrendMetricRepository trendMetricRepository;
    private final FinanceInstrumentClient financeInstrumentClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to) {
        String normalizedSymbol = requireSymbol(symbol);
        validateDateRange(from, to);
        return financeInstrumentClient.resolveInstrumentId(normalizedSymbol)
                .map(instrumentId -> loadDailyCandles(instrumentId, from, to))
                .orElseGet(() -> loadDailyCandlesBySymbol(normalizedSymbol, from, to))
                .stream()
                .sorted(Comparator.comparing(AnalyticsPriceCandleDaily::getCandleDate))
                .map(CandleResponse::from)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CandleResponse> getCandlesByInterval(
            String symbol,
            CandleInterval interval,
            LocalDate from,
            LocalDate to
    ) {
        String normalizedSymbol = requireSymbol(symbol);
        validateDateRange(from, to);
        if (interval == null) {
            throw new IllegalArgumentException("interval is required");
        }
        return financeInstrumentClient.resolveInstrumentId(normalizedSymbol)
                .map(instrumentId -> loadIntervalCandles(instrumentId, interval, from, to))
                .orElseGet(() -> loadIntervalCandlesBySymbol(normalizedSymbol, interval, from, to))
                .stream()
                .sorted(Comparator.comparing(AnalyticsPriceCandle::getOpenTime))
                .map(CandleResponse::from)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MovingAverageResponse> getMovingAverage(String symbol) {
        String normalizedSymbol = requireSymbol(symbol);
        return movingAverageRepository
                .findByInstrumentSymbol(normalizedSymbol)
                .stream()
                .map(m ->
                        MovingAverageResponse.builder()
                                .symbol(m.getInstrumentSymbol())
                                .date(m.getTradeDate())
                                .ma7(m.getMa7())
                                .ma30(m.getMa30())
                                .ma90(m.getMa90())
                                .build()
                )
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RSIResponse> getRSI(String symbol) {
        String normalizedSymbol = requireSymbol(symbol);
        return rsiRepository
                .findByInstrumentSymbolOrderByTradeDateAsc(normalizedSymbol)
                .stream()
                .map(r ->
                        RSIResponse.builder()
                                .symbol(r.getInstrumentSymbol())
                                .date(r.getTradeDate())
                                .rsi14(r.getRsi14())
                                .build()
                )
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TrendMetricResponse> getTrendMetrics(String symbol) {
        String normalizedSymbol = requireSymbol(symbol);
        return trendMetricRepository
                .findByInstrumentSymbolOrderByTradeDateAsc(normalizedSymbol)
                .stream()
                .map(TrendMetricResponse::from)
                .toList();
    }

    private List<AnalyticsPriceCandleDaily> loadDailyCandles(Long instrumentId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return CandleQuerySupport.dedupeDailyCandlesByDate(
                    candleRepository.findByInstrumentIdOrderByCandleDateAsc(instrumentId)
            );
        }
        return CandleQuerySupport.dedupeDailyCandlesByDate(
                candleRepository.findByInstrumentIdAndCandleDateBetweenOrderByCandleDateAsc(instrumentId, from, to)
        );
    }

    private List<AnalyticsPriceCandleDaily> loadDailyCandlesBySymbol(String symbol, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return CandleQuerySupport.dedupeDailyCandlesByDate(
                    candleRepository.findByInstrumentSymbolOrderByCandleDateAsc(symbol)
            );
        }
        return CandleQuerySupport.dedupeDailyCandlesByDate(
                candleRepository.findByInstrumentSymbolAndCandleDateBetweenOrderByCandleDateAsc(symbol, from, to)
        );
    }

    private List<AnalyticsPriceCandle> loadIntervalCandles(
            Long instrumentId,
            CandleInterval interval,
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            return CandleQuerySupport.dedupeIntervalCandlesByOpenTime(
                    multiIntervalCandleRepository.findByInstrumentIdAndCandleIntervalOrderByOpenTimeAsc(
                            instrumentId, interval
                    )
            );
        }
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(1);
        return CandleQuerySupport.dedupeIntervalCandlesByOpenTime(
                multiIntervalCandleRepository.findByInstrumentIdAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                        instrumentId, interval, fromInstant, toInstant
                )
        );
    }

    private List<AnalyticsPriceCandle> loadIntervalCandlesBySymbol(
            String symbol,
            CandleInterval interval,
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            return CandleQuerySupport.dedupeIntervalCandlesByOpenTime(
                    multiIntervalCandleRepository.findByInstrumentSymbolAndCandleIntervalOrderByOpenTimeAsc(symbol, interval)
            );
        }
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(1);
        return CandleQuerySupport.dedupeIntervalCandlesByOpenTime(
                multiIntervalCandleRepository.findByInstrumentSymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                        symbol, interval, fromInstant, toInstant
                )
        );
    }

    private static String requireSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return symbol.trim();
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }
}
