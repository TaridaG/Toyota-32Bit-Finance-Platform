package com.company.finance_api.news.infrastructure.http.dto;

/** NewsWeeklyTopicRowResponse — API transfer nesnesi (DTO/response/request). */
public record NewsWeeklyTopicRowResponse(String key, long count, int percent) {}
