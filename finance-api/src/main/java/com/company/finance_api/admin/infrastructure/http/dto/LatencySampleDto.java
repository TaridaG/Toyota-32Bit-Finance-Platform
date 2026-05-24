package com.company.finance_api.admin.infrastructure.http.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/** Latency probe tek örnek: path ve süre (ms). */
public record LatencySampleDto(
    @NotBlank String path, @DecimalMin(value = "0.0", inclusive = true) double durationMs) {}
