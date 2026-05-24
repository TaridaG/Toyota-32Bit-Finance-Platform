package com.company.finance_api.admin.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Market-asset analytics snapshot ve sayfalı instrument tablosu. */
public record AdminMarketAssetDashboardDto(
    String status,
    Instant startedAt,
    Instant computedAt,
    BigDecimal avgWatchlistInstrumentsPerUser,
    BigDecimal avgInstrumentsPerPortfolio,
    BigDecimal avgPortfolioWeightPercent,
    int instrumentRowCount,
    String errorMessage,
    AdminMarketAssetStatsPageDto table) {}
