package com.company.finance_api.portfolio.external.application;

import com.company.finance_api.portfolio.external.infrastructure.http.dto.ExternalPortfolioAllocationResponse;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.ExternalPortfolioSummaryResponse;
import java.util.List;
import java.util.UUID;

/** External portfolio değerleme ve allocation hesaplamaları için service arayüzü. */
public interface ExternalPortfolioValuationService {

  /** Portfolio değerleme özetini hesaplar. */
  ExternalPortfolioSummaryResponse calculateSummary(UUID userId, Long portfolioId);

  /** Portfolio varlık dağılımını hesaplar. */
  List<ExternalPortfolioAllocationResponse> calculateAllocation(UUID userId, Long portfolioId);
}
