package com.company.marketdataservice.viop.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * VIOP sözleşme settlement geçmişi için API transfer nesnesi (DTO/response).
 */
public record ViopSettlementHistoryPointDto(
        LocalDate tradeDate,
        BigDecimal settlementPrice
) {}

