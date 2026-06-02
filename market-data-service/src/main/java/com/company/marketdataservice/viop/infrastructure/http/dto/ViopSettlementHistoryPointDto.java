package com.company.marketdataservice.viop.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ViopSettlementHistoryPointDto(
        LocalDate tradeDate,
        BigDecimal settlementPrice
) {}

