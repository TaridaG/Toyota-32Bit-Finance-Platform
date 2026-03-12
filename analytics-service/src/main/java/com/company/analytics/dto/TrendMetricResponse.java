package com.company.analytics.dto;

import com.company.analytics.domain.AnalyticsTrendMetric;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class TrendMetricResponse {

    private String symbol;
    private LocalDate date;
    private String trendDirection;
    private BigDecimal momentum;
    private BigDecimal priceSlope;

    public static TrendMetricResponse from(AnalyticsTrendMetric metric) {
        return TrendMetricResponse.builder()
                .symbol(metric.getInstrumentSymbol())
                .date(metric.getTradeDate())
                .trendDirection(metric.getTrendDirection().name())
                .momentum(metric.getMomentum())
                .priceSlope(metric.getPriceSlope())
                .build();
    }
}