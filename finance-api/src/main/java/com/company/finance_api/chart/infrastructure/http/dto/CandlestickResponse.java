package com.company.finance_api.chart.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** CandlestickResponse — API transfer nesnesi (DTO/response/request). */
public record CandlestickResponse(
    Instant time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
