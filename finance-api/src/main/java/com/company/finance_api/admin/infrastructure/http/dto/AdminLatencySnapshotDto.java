package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;

/** Admin dashboard KPI için en son probe özeti. */
public record AdminLatencySnapshotDto(
    long id, double averageLatencySec, int sampleCount, Instant measuredAt, boolean hasSamples) {}
