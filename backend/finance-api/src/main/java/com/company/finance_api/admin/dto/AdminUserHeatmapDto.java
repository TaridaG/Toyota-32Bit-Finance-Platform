package com.company.finance_api.admin.dto;

import java.util.List;

public record AdminUserHeatmapDto(boolean available, List<AdminUserHeatmapCellDto> cells) {
    public static AdminUserHeatmapDto notTracked() {
        return new AdminUserHeatmapDto(false, List.of());
    }
}
