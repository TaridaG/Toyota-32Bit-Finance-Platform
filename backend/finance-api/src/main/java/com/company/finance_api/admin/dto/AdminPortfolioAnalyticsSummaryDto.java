package com.company.finance_api.admin.dto;

public record AdminPortfolioAnalyticsSummaryDto(
        long totalPortfolios,
        long portfoliosCreatedPreviousIsoWeekUtc,
        long portfoliosCreatedPreviousCalendarMonthUtc,
        /** Non-deleted position lots / portfolio count (empty portfolios lower the average). */
        double averageOpenLotsPerPortfolio,
        /** External portfolios per roster user (users not in deletion workflow). */
        double portfoliosPerRosterUser,
        long rosterUsersTotal,
        long openPositionLotsTotal,
        long portfoliosCreatedYesterdayUtc,
        long portfoliosCreatedDayBeforeYesterdayUtc,
        /** Approx. day-over-day % on total count from yesterday creations (see service). */
        double totalPortfoliosVsPriorDayPercentApprox
) {}
