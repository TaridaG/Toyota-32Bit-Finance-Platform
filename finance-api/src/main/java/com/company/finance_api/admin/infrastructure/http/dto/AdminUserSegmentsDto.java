package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Kullanıcı segmentasyon kovaları (telemetry yoksa notTracked). */
public record AdminUserSegmentsDto(boolean available, List<AdminUserSegmentBucketDto> buckets) {
  /** Segmentasyon henüz izlenmediğinde placeholder döner. */
  public static AdminUserSegmentsDto notTracked() {
    return new AdminUserSegmentsDto(false, List.of());
  }
}
