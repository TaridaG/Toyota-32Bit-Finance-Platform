package com.company.finance_api.market.eurobond.yahoo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** EurobondYahooIngestScheduler — zamanlanmış batch görevi. */
@Component
@ConditionalOnProperty(
    prefix = "market.tr-usd-eurobond-yahoo",
    name = "enabled",
    havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class EurobondYahooIngestScheduler {

  private final EurobondYahooHistoryIngestService ingestService;

  @Scheduled(
      cron = "${market.tr-usd-eurobond-yahoo.refresh-cron}",
      zone = "${market.tr-usd-eurobond-yahoo.trading-timezone}")
  public void scheduledRefresh() {
    try {
      ingestService.refreshAllConfigured();
    } catch (Exception ex) {
      log.warn("EUROBOND_YAHOO_SCHEDULER_FAIL reason={}", ex.toString());
    }
  }
}
