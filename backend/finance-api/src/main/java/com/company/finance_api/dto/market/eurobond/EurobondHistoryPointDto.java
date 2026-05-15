package com.company.finance_api.dto.market.eurobond;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EurobondHistoryPointDto(
        LocalDate date,
        BigDecimal closePrice,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closeYieldPercent,
        BigDecimal openYieldPercent,
        BigDecimal highYieldPercent,
        BigDecimal lowYieldPercent,
        BigDecimal changePercent,
        String sourceProvider
) {
}
