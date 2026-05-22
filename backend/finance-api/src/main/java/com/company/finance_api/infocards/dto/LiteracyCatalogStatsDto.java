package com.company.finance_api.infocards.dto;

public record LiteracyCatalogStatsDto(
        long total,
        long terms,
        long charts,
        long analysisTools,
        long macro
) {
}
