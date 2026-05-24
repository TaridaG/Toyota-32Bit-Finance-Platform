package com.company.finance_api.market.eurobond.yahoo;

import com.company.finance_api.bootstrap.config.MarketTrUsdEurobondYahooProperties;
import com.company.finance_api.bootstrap.config.MarketTrUsdEurobondYahooProperties.InstrumentRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Logs once why eurobond Yahoo ingest may be inactive (defaults are off + empty symbol list). */
@Component
@RequiredArgsConstructor
@Slf4j
public class EurobondYahooIngestConfigurationLogger {

  private final MarketTrUsdEurobondYahooProperties properties;

  @Order(Integer.MAX_VALUE)
  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    long withSymbol = properties.getInstruments().stream().filter(this::rowHasSymbol).count();
    log.info(
        "EUROBOND_YAHOO_CONFIG enabled={} rowsWithYahooSymbol={} totalInstrumentRows={} "
            + "(set MARKET_TR_USD_EUROBOND_YAHOO_ENABLED=true and market.tr-usd-eurobond-yahoo.instruments with yahoo-chart-symbol)",
        properties.isEnabled(),
        withSymbol,
        properties.getInstruments().size());
  }

  private boolean rowHasSymbol(InstrumentRow row) {
    return row != null
        && StringUtils.hasText(row.getIsin())
        && StringUtils.hasText(row.getYahooChartSymbol());
  }
}
