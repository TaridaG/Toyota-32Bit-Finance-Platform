package com.company.analytics.query.infrastructure.http.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Moving average (MA7, MA30, MA90) değerlerini REST yanıtında taşıyan DTO.
 */
@Getter
@Builder
public class MovingAverageResponse {

    private String symbol;

    private LocalDate date;

    private BigDecimal ma7;

    private BigDecimal ma30;

    private BigDecimal ma90;

}
