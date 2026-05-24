package com.company.finance_api.admin.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Market-asset analytics yeniden hesaplamasını async task executor'da çalıştırır. */
@Component
public class AdminMarketAssetRecomputeWorker {

  private static final Logger log = LoggerFactory.getLogger(AdminMarketAssetRecomputeWorker.class);

  private final AdminMarketAssetAnalyticsService analyticsService;

  public AdminMarketAssetRecomputeWorker(AdminMarketAssetAnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  /** Recompute işlemini arka planda yürütür; hata durumunda snapshot'ı FAILED yapar. */
  @Async("adminMarketAssetTaskExecutor")
  public void run() {
    try {
      analyticsService.recomputeAndPersist();
    } catch (RuntimeException ex) {
      log.error("Admin market asset recompute failed", ex);
      analyticsService.markFailed(ex.getMessage());
    } finally {
      analyticsService.releaseRecomputeLock();
    }
  }
}
