package com.company.analytics.dto;

import com.company.analytics.domain.AnalyticsPriceCandleDaily;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CandleResponse {

    private String symbol;
    private LocalDate candleDate;
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
}