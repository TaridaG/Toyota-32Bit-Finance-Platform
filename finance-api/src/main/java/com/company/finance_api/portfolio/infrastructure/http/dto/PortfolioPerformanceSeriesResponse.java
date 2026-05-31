package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.time.LocalDate;
import java.util.List;

/** Portfolio chart response with inception-aware daily points. */
public record PortfolioPerformanceSeriesResponse(
    String currency, LocalDate inceptionDay, List<PortfolioPerformancePointResponse> points) {}
