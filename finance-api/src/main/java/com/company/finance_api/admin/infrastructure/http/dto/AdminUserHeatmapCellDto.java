package com.company.finance_api.admin.infrastructure.http.dto;

/** Heatmap tek hücre: haftanın günü, saat ve değer. */
public record AdminUserHeatmapCellDto(int dayOfWeek, int hour, long value) {}
