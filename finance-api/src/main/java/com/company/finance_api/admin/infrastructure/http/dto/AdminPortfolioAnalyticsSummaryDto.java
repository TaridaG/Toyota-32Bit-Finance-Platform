package com.company.finance_api.admin.infrastructure.http.dto;

/** Portfolio analytics KPI özet bloğu. */
public record AdminPortfolioAnalyticsSummaryDto(
    long totalPortfolios,
    long portfoliosCreatedPreviousIsoWeekUtc,
    long portfoliosCreatedPreviousCalendarMonthUtc,
    /** Silinmemiş position lot / portfolio oranı (boş portfolio'lar ortalamayı düşürür). */
    double averageOpenLotsPerPortfolio,
    /** Roster kullanıcı başına external portfolio (silme workflow'unda olmayanlar). */
    double portfoliosPerRosterUser,
    long rosterUsersTotal,
    long openPositionLotsTotal,
    long portfoliosCreatedYesterdayUtc,
    long portfoliosCreatedDayBeforeYesterdayUtc,
    /** Dünkü oluşturmalara göre toplam sayıda yaklaşık gün/gün % (bkz. service). */
    double totalPortfoliosVsPriorDayPercentApprox) {}
