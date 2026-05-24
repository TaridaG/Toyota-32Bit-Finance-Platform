package com.company.finance_api.dto;

import java.time.Instant;

/** InstrumentPriceCoverageResponse — API transfer nesnesi (DTO/response/request). */
public record InstrumentPriceCoverageResponse(
    Long instrumentId, String symbol, Instant firstAvailableAt, Instant lastAvailableAt) {}
