package com.company.analytics.query.infrastructure.http.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RSI-14 değerlerini REST yanıtında taşıyan DTO.
 */
@Getter
@Builder
public class RSIResponse {

    private String symbol;

    private LocalDate date;

    private BigDecimal rsi14;

}
