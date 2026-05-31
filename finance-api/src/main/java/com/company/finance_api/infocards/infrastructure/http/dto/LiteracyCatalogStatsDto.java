package com.company.finance_api.infocards.infrastructure.http.dto;

/** Literacy catalog istatistik özeti. */
public record LiteracyCatalogStatsDto(
    long total, long terms, long charts, long analysisTools, long macro) {}
