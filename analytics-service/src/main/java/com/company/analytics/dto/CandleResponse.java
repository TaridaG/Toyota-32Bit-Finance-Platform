package com.company.analytics.dto;

import com.company.analytics.domain.AnalyticsPriceCandle;
import com.company.analytics.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.domain.enums.CandleInterval;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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