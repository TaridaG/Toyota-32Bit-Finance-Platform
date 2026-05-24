package com.company.finance_api.admin.infrastructure.http.dto;

/** Ürün analytics (session, page view) — telemetry pipeline olduğunda doldurulur. */
public record AdminUserInteractionMetricsDto(
    boolean measured,
    Long averageSessionDurationSeconds,
    Long sessionCount,
    Long pageViews,
    Double bounceRatePercent) {
  /** Telemetry olmadığında ölçülmemiş etkileşim metrikleri döner. */
  public static AdminUserInteractionMetricsDto notMeasured() {
    return new AdminUserInteractionMetricsDto(false, null, null, null, null);
  }
}
