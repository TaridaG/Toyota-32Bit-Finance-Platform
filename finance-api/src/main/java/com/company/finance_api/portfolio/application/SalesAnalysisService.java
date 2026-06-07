package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.SalesAnalysisPageResponse;
import java.time.LocalDate;

/** Portfolio UI için kapanmış satış analytics iş mantığı (sales analysis service). */
public interface SalesAnalysisService {

  /** Oturum açmış kullanıcının SELL transaction'larını sayfalı analytics satırları olarak döner. */
  SalesAnalysisPageResponse getMySalesAnalysisPage(
      int page,
      int size,
      Long portfolioId,
      String symbol,
      LocalDate fromDate,
      LocalDate toDate);
}
