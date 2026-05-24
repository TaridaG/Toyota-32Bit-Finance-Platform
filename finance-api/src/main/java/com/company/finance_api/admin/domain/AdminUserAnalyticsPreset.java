package com.company.finance_api.admin.domain;

import java.util.Locale;

/**
 * Admin kullanıcı analytics grafikleri ve dönem KPI'ları için UTC takvim hizalı preset pencereleri.
 */
public enum AdminUserAnalyticsPreset {
  LAST_7_DAYS("7d", 7),
  LAST_14_DAYS("14d", 14),
  LAST_30_DAYS("30d", 30);

  private final String queryParam;
  private final int inclusiveDayCount;

  AdminUserAnalyticsPreset(String queryParam, int inclusiveDayCount) {
    this.queryParam = queryParam;
    this.inclusiveDayCount = inclusiveDayCount;
  }

  /** REST query parametresi değerini döner. */
  public String queryParam() {
    return queryParam;
  }

  /** Grafikteki UTC takvim gün sayısı (bugün dahil). */
  public int inclusiveDayCount() {
    return inclusiveDayCount;
  }

  /** Ham preset string'ini güvenli şekilde çözümler; bilinmeyen değerde varsayılan döner. */
  public static AdminUserAnalyticsPreset parse(String raw) {
    if (raw == null) {
      return LAST_7_DAYS;
    }
    String s = raw.trim().toLowerCase(Locale.ROOT);
    for (AdminUserAnalyticsPreset p : values()) {
      if (p.queryParam.equals(s)) {
        return p;
      }
    }
    return LAST_7_DAYS;
  }
}
