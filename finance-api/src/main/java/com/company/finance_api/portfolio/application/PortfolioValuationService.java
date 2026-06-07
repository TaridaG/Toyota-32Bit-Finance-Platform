package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioValuationResponse;

/** PortfolioValuationService iş mantığını uygular (portfolio valuation service). */
public interface PortfolioValuationService {

  /** Oturum açmış kullanıcının güncel portfolio değerlemesini döner. */
  PortfolioValuationResponse getMyValuation();
}
