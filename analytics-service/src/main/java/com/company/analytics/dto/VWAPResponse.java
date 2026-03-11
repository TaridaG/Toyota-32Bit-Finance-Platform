package com.company.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class VWAPResponse {

    private String symbol;

    private LocalDate date;

    private BigDecimal vwap;

}