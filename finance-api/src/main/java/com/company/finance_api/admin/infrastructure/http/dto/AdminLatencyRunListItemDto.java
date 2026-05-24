package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;

/** Latency probe run listesinde tek satır. */
public record AdminLatencyRunListItemDto(
    long id, double averageLatencySec, int sampleCount, Instant measuredAt, boolean hasSamples) {}
