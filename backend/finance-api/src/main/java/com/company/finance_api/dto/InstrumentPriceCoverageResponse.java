package com.company.finance_api.dto;

import java.time.Instant;

public record InstrumentPriceCoverageResponse(
        Long instrumentId,
        String symbol,
        Instant firstAvailableAt,
        Instant lastAvailableAt
) {
}

