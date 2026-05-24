package com.company.finance_api.admin.infrastructure.http.dto;

/** Tek UTC günü: yeni oluşturulan external portfolio sayısı ({@code created_at}). */
public record AdminPortfolioDailyCreationDto(
    String date, int portfoliosCreated, double rollingAverage7d, int deltaVsPreviousDay) {}
