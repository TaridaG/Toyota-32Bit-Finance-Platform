package com.company.analytics.dto;

import com.company.analytics.domain.AnalyticsTradeAggregateDaily;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class AnalyticsSummaryResponse {

    private String symbol;

    private LocalDate tradeDate;

    private Long tradeCount;

    private BigDecimal volume;

    public static AnalyticsSummaryResponse from(
            AnalyticsTradeAggregateDaily agg
    ) {
        return AnalyticsSummaryResponse.builder()
                .symbol(agg.getInstrumentSymbol())
                .tradeDate(agg.getTradeDate())
                .tradeCount(agg.getTradeCount())
                .volume(agg.getTotalVolume())
                .build();
    }
}