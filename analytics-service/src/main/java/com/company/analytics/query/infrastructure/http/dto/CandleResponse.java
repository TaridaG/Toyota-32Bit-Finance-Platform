package com.company.analytics.query.infrastructure.http.dto;

import com.company.analytics.processing.domain.AnalyticsPriceCandle;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.enums.CandleInterval;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Candle verilerini REST yanıtında taşıyan DTO.
 * Günlük ve çoklu interval candle kayıtlarını tek bir yapıda sunar.
 */
@Getter
@Builder
public class CandleResponse {

    private String symbol;
    private LocalDate candleDate;
    private CandleInterval interval;
    private Instant openTime;
    private Instant closeTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private Long tradeCount;

    /**
     * Günlük candle domain entity'sinden DTO oluşturur.
     *
     * @param candle günlük candle kaydı
     * @return candle DTO
     */
    public static CandleResponse from(AnalyticsPriceCandleDaily candle) {
        return CandleResponse.builder()
                .symbol(candle.getInstrumentSymbol())
                .candleDate(candle.getCandleDate())
                .open(candle.getOpenPrice())
                .high(candle.getHighPrice())
                .low(candle.getLowPrice())
                .close(candle.getClosePrice())
                .volume(candle.getVolume())
                .tradeCount(candle.getTradeCount())
                .build();
    }

    /**
     * Çoklu interval candle domain entity'sinden DTO oluşturur.
     * {@code closeTime}, interval süresine göre {@code openTime} üzerinden hesaplanır.
     *
     * @param candle çoklu interval candle kaydı
     * @return candle DTO
     */
    public static CandleResponse from(AnalyticsPriceCandle candle) {
        return CandleResponse.builder()
                .symbol(candle.getInstrumentSymbol())
                .interval(candle.getCandleInterval())
                .openTime(candle.getOpenTime())
                .closeTime(resolveCloseTime(candle.getOpenTime(), candle.getCandleInterval()))
                .open(candle.getOpenPrice())
                .high(candle.getHighPrice())
                .low(candle.getLowPrice())
                .close(candle.getClosePrice())
                .volume(candle.getVolume())
                .tradeCount(candle.getTradeCount())
                .build();
    }

    private static Instant resolveCloseTime(Instant openTime, CandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> openTime.plusSeconds(60);
            case FIVE_MINUTES -> openTime.plusSeconds(300);
            case ONE_HOUR -> openTime.plusSeconds(3600);
            case ONE_DAY -> openTime.plusSeconds(86400);
        };
    }
}
