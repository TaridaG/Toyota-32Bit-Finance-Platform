package com.company.reporting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InstrumentReportData {

    private String symbol;
    private List<AnalyticsCandleDto> candles;
    private List<AnalyticsMovingAverageDto> movingAverages;
    private List<AnalyticsTrendMetricDto> trendMetrics;
}