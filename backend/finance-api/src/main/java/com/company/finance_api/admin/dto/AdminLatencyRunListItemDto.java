package com.company.finance_api.admin.dto;

import java.time.Instant;

public record AdminLatencyRunListItemDto(
        long id,
        double averageLatencySec,
        int sampleCount,
        Instant measuredAt,
        boolean hasSamples
) {
}
