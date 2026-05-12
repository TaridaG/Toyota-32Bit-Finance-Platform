package com.company.finance_api.admin.dto;

import java.time.Instant;

/** Latest probe summary for the admin dashboard KPI. */
public record AdminLatencySnapshotDto(
        long id,
        double averageLatencySec,
        int sampleCount,
        Instant measuredAt,
        boolean hasSamples
) {
}
