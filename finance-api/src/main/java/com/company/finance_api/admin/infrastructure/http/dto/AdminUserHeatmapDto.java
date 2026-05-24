package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Kullanıcı aktivite heatmap verisi (telemetry yoksa notTracked). */
public record AdminUserHeatmapDto(boolean available, List<AdminUserHeatmapCellDto> cells) {
  /** Heatmap henüz izlenmediğinde placeholder döner. */
  public static AdminUserHeatmapDto notTracked() {
    return new AdminUserHeatmapDto(false, List.of());
  }
}
