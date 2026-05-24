package com.company.finance_api.admin.infrastructure.http.dto;

/** Tek probe path için süre örneği (ms). */
public record AdminLatencyProbeSampleItemDto(String path, double durationMs) {}
