package com.company.finance_api.bootstrap.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** TR USD eurobond Yahoo chart ingestion ayarları ({@code market.tr-usd-eurobond-yahoo}). */
@ConfigurationProperties(prefix = "market.tr-usd-eurobond-yahoo")
public class MarketTrUsdEurobondYahooProperties {

  private boolean enabled;
  private String userAgent =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
  private String baseUrl = "https://query1.finance.yahoo.com";
  private String chartPathTemplate = "/v8/finance/chart/{symbol}";
  private String sourceProviderId = "YAHOO";

  /** "Bugün zaten ingest edildi mi?" ve Yahoo bar zamanı yoksa quote timestamp için kullanılır. */
  private String tradingTimezone = "Europe/Istanbul";

  private boolean backfillOnStartup = true;
  private String refreshCron = "0 30 18 * * MON-FRI";
  private String backfillRange = "1y";
  private String catchUpRange = "1mo";
  private String interval = "1d";
  private long requestSpacingMs = 400;

  /**
   * {@code true} iken her serinin Yahoo OHLC değerleri sabit clean-price bandına (varsayılan
   * 94–106) map edilir; GOV serisi bloklandığında ETF proxy'leri için UI/kupon hesabı bond benzeri
   * ölçekte kalır.
   */
  private boolean normalizeChartToBondPriceWindow = false;

  private double normalizeTargetLow = 94.0d;
  private double normalizeTargetHigh = 106.0d;

  private List<InstrumentRow> instruments = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getChartPathTemplate() {
    return chartPathTemplate;
  }

  public void setChartPathTemplate(String chartPathTemplate) {
    this.chartPathTemplate = chartPathTemplate;
  }

  public String getSourceProviderId() {
    return sourceProviderId;
  }

  public void setSourceProviderId(String sourceProviderId) {
    this.sourceProviderId = sourceProviderId;
  }

  public String getTradingTimezone() {
    return tradingTimezone;
  }

  public void setTradingTimezone(String tradingTimezone) {
    this.tradingTimezone = tradingTimezone;
  }

  public boolean isBackfillOnStartup() {
    return backfillOnStartup;
  }

  public void setBackfillOnStartup(boolean backfillOnStartup) {
    this.backfillOnStartup = backfillOnStartup;
  }

  public String getRefreshCron() {
    return refreshCron;
  }

  public void setRefreshCron(String refreshCron) {
    this.refreshCron = refreshCron;
  }

  public String getBackfillRange() {
    return backfillRange;
  }

  public void setBackfillRange(String backfillRange) {
    this.backfillRange = backfillRange;
  }

  public String getCatchUpRange() {
    return catchUpRange;
  }

  public void setCatchUpRange(String catchUpRange) {
    this.catchUpRange = catchUpRange;
  }

  public String getInterval() {
    return interval;
  }

  public void setInterval(String interval) {
    this.interval = interval;
  }

  public long getRequestSpacingMs() {
    return requestSpacingMs;
  }

  public void setRequestSpacingMs(long requestSpacingMs) {
    this.requestSpacingMs = requestSpacingMs;
  }

  public boolean isNormalizeChartToBondPriceWindow() {
    return normalizeChartToBondPriceWindow;
  }

  public void setNormalizeChartToBondPriceWindow(boolean normalizeChartToBondPriceWindow) {
    this.normalizeChartToBondPriceWindow = normalizeChartToBondPriceWindow;
  }

  public double getNormalizeTargetLow() {
    return normalizeTargetLow;
  }

  public void setNormalizeTargetLow(double normalizeTargetLow) {
    this.normalizeTargetLow = normalizeTargetLow;
  }

  public double getNormalizeTargetHigh() {
    return normalizeTargetHigh;
  }

  public void setNormalizeTargetHigh(double normalizeTargetHigh) {
    this.normalizeTargetHigh = normalizeTargetHigh;
  }

  public List<InstrumentRow> getInstruments() {
    return instruments;
  }

  public void setInstruments(List<InstrumentRow> instruments) {
    this.instruments = instruments != null ? instruments : new ArrayList<>();
  }

  /** ISIN → Yahoo chart symbol eşleştirmesi. */
  public static final class InstrumentRow {
    private String isin;
    private String yahooChartSymbol;

    public String getIsin() {
      return isin;
    }

    public void setIsin(String isin) {
      this.isin = isin;
    }

    public String getYahooChartSymbol() {
      return yahooChartSymbol;
    }

    public void setYahooChartSymbol(String yahooChartSymbol) {
      this.yahooChartSymbol = yahooChartSymbol;
    }
  }
}
