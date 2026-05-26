package com.company.finance_api.scheduler;

import com.company.finance_api.portfolio.application.PortfolioPerformanceSeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically refreshes precomputed daily portfolio performance rows. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioPerformanceSeriesScheduler {

  private final PortfolioPerformanceSeriesService portfolioPerformanceSeriesService;

  @Scheduled(fixedDelayString = "${portfolio.performance.refresh-ms:3600000}")
  public void refresh() {
    log.info("Refreshing portfolio daily performance rows...");
    portfolioPerformanceSeriesService.recomputeAllPortfolioHistory();
  }
}
