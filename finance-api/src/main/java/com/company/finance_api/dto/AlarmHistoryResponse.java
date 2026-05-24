package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** AlarmHistoryResponse — API transfer nesnesi (DTO/response/request). */
public record AlarmHistoryResponse(
    String instrumentSymbol, String condition, BigDecimal price, Instant triggeredAt) {}
