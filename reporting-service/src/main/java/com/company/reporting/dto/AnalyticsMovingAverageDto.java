package com.company.reporting.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AnalyticsMovingAverageDto {
    private String symbol;
    private LocalDate date;
    private BigDecimal ma7;
    private BigDecimal ma30;
    private BigDecimal ma90;
}