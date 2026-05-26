package com.company.finance_api.chart.infrastructure.http.dto;

import com.company.finance_api.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

/** TradeMarkerResponse — API transfer nesnesi (DTO/response/request). */
public record TradeMarkerResponse(
    Instant time, TransactionType type, BigDecimal price, BigDecimal quantity) {}
