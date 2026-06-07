package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioOverviewResponse;

/** PortfolioOverviewService iş mantığını uygular (portfolio overview service). */
public interface PortfolioOverviewService {
  /** Hedef para biriminde portfolio genel bakışını döner. */
  PortfolioOverviewResponse getMyOverview(String targetCurrency, Long portfolioId);

  /** Ledger'dan as-of tarihindeki pozisyonları döner (satış günü alımları dahil değil). */
  PortfolioOverviewResponse getHoldingsAsOf(
      String targetCurrency, Long portfolioId, java.time.LocalDate asOfDate);
}
