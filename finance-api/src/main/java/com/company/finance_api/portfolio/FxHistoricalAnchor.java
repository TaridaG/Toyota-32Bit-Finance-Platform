package com.company.finance_api.portfolio;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Takvim günü edinim anını gün sonu UTC ile hizalar; UI gece yarısı UTC gönderdiğinde MDS/FX "at or
 * before" sorgularının tüm işlem gününü kapsamasını sağlar.
 */
public final class FxHistoricalAnchor {

  private FxHistoricalAnchor() {}

  /**
   * {@code acquiredAt} tam gece yarısı UTC ise o günün son nanosaniyesini döner; aksi halde
   * değişmeden döner.
   */
  public static Instant normalizeEndOfAcquisitionDay(Instant acquiredAt) {
    ZonedDateTime utc = acquiredAt.atZone(ZoneOffset.UTC);
    if (utc.getHour() == 0 && utc.getMinute() == 0 && utc.getSecond() == 0 && utc.getNano() == 0) {
      return utc.plusDays(1).minusNanos(1).toInstant();
    }
    return acquiredAt;
  }
}
