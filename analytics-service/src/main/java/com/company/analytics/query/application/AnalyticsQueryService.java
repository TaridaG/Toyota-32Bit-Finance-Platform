package com.company.analytics.query.application;

import com.company.analytics.processing.domain.enums.CandleInterval;
import com.company.analytics.query.infrastructure.http.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Analytics sorgu katmanı servis arayüzü.
 * Enstrüman bazlı candle, moving average, RSI ve trend metric verilerini okur.
 */
public interface AnalyticsQueryService {

    /**
     * Günlük candle kayıtlarını sembol ve tarih aralığına göre query eder.
     *
     * @param symbol enstrüman sembolü
     * @param from   başlangıç tarihi; {@code null} ise tüm kayıtlar
     * @param to     bitiş tarihi; {@code null} ise tüm kayıtlar
     * @return candle DTO listesi
     */
    List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to);

    /**
     * Moving average (MA7, MA30, MA90) değerlerini sembol için döner.
     *
     * @param symbol enstrüman sembolü
     * @return moving average DTO listesi
     */
    List<MovingAverageResponse> getMovingAverage(String symbol);

    /**
     * RSI-14 değerlerini sembol için döner.
     *
     * @param symbol enstrüman sembolü
     * @return RSI DTO listesi
     */
    List<RSIResponse> getRSI(String symbol);

    /**
     * Trend metric kayıtlarını sembol için döner.
     *
     * @param symbol enstrüman sembolü
     * @return trend metric DTO listesi
     */
    List<TrendMetricResponse> getTrendMetrics(String symbol);

    /**
     * Belirtilen candle interval'ına göre candle kayıtlarını query eder.
     *
     * @param symbol   enstrüman sembolü
     * @param interval candle interval (ONE_MINUTE, FIVE_MINUTES, vb.)
     * @param from     başlangıç tarihi; {@code null} ise tüm kayıtlar
     * @param to       bitiş tarihi; {@code null} ise tüm kayıtlar
     * @return candle DTO listesi
     */
    List<CandleResponse> getCandlesByInterval(
            String symbol,
            CandleInterval interval,
            LocalDate from,
            LocalDate to
    );
}
