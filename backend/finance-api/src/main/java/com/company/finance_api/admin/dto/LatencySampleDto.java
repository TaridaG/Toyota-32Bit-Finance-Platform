package com.company.finance_api.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record LatencySampleDto(
        @NotBlank String path,
        @DecimalMin(value = "0.0", inclusive = true) double durationMs
) {
}
