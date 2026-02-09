package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CandlestickResponse(
        Instant time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close
) {}