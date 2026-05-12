package com.company.finance_api.admin.dto;

/** One UTC calendar day: new external portfolios created (by {@code created_at}). */
public record AdminPortfolioDailyCreationDto(
        String date,
        int portfoliosCreated,
        double rollingAverage7d,
        int deltaVsPreviousDay
) {}
