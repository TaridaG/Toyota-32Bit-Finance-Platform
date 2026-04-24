package com.company.finance_api.dto;

import java.util.Map;

public record InsightDto(
        String type,
        String message,
        InsightSeverity severity,
        Map<String, Object> metadata
) {
}
