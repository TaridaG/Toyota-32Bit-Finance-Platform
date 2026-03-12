package com.company.reporting.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AnalyticsTrendMetricDto {
    private String symbol;
    private LocalDate date;
    private String trendDirection;
    private BigDecimal momentum;
    private BigDecimal priceSlope;
}