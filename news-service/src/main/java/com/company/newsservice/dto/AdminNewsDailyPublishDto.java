package com.company.newsservice.dto;

/** One UTC calendar day in the admin chart (active articles by {@code publishedAt}). */
public record AdminNewsDailyPublishDto(
        String date,
        int articlesPublished,
        double rollingAverage7d,
        int deltaVsPreviousDay
) {}
