package com.company.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class MovingAverageResponse {

    private String symbol;

    private LocalDate date;

    private BigDecimal ma7;

    private BigDecimal ma30;

    private BigDecimal ma90;

}