package com.company.marketdataservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoryPointDto(
        Instant time,
        BigDecimal value
) {
}
