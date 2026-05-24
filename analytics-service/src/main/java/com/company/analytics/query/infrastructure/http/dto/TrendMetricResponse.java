package com.company.analytics.query.infrastructure.http.dto;

import com.company.analytics.processing.domain.AnalyticsTrendMetric;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Trend metric verilerini REST yanıtında taşıyan DTO.
 * Trend yönü, momentum ve fiyat eğimi bilgilerini içerir.
 */
@Getter
@Builder
public class TrendMetricResponse {

    private String symbol;
    private LocalDate date;
    private String trendDirection;
    private BigDecimal momentum;
    private BigDecimal priceSlope;

    /**
     * Trend metric domain entity'sinden DTO oluşturur.
     *
     * @param metric trend metric kaydı
     * @return trend metric DTO
     */
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
