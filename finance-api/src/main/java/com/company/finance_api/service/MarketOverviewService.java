package com.company.finance_api.service;

import com.company.finance_api.dto.MarketOverviewPageResponse;

/** Piyasa özeti ve insight sorgularını fiyat snapshot'ları ile birleştirir. */
public interface MarketOverviewService {

  /** Sayfalanmış piyasa özet listesini döner. */
  MarketOverviewPageResponse getOverview(
      int page, int size, String category, String search, String targetCurrency, String sort);
}
