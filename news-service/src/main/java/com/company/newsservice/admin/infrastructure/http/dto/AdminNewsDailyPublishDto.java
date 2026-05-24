package com.company.newsservice.admin.infrastructure.http.dto;

/** Admin chart'ta tek bir UTC calendar günü ({@code publishedAt} ile active article'lar). */
public record AdminNewsDailyPublishDto(
        String date,
        int articlesPublished,
        double rollingAverage7d,
        int deltaVsPreviousDay
) {}
