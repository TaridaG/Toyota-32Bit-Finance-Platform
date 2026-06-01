package com.company.marketdataservice.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sık okunan market endpoint'leri için bellek içi cache ayarları ({@code market.hot-read-cache}).
 * Fiyat, FX ve pulse yanıtları için ayrı TTL ve genel {@code enabled} bayrağı.
 */
@ConfigurationProperties(prefix = "market.hot-read-cache")
public class HotReadCacheProperties {

  private boolean enabled = true;
  private long pricesTtlMs = 3_000L;
  private long fxTtlMs = 5_000L;
  private long pulseTtlMs = 30_000L;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getPricesTtlMs() {
    return pricesTtlMs;
  }

  public void setPricesTtlMs(long pricesTtlMs) {
    this.pricesTtlMs = pricesTtlMs;
  }

  public long getFxTtlMs() {
    return fxTtlMs;
  }

  public void setFxTtlMs(long fxTtlMs) {
    this.fxTtlMs = fxTtlMs;
  }

  public long getPulseTtlMs() {
    return pulseTtlMs;
  }

  public void setPulseTtlMs(long pulseTtlMs) {
    this.pulseTtlMs = pulseTtlMs;
  }
}
