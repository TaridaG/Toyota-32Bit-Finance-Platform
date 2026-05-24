package com.company.finance_api.portfolio.external.service;

import com.company.finance_api.portfolio.external.dto.ExternalPortfolioAllocationResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;
import java.util.List;
import java.util.UUID;

/** External portfolio değerleme ve allocation hesaplamaları için service arayüzü. */
public interface ExternalPortfolioValuationService {

  /** Portfolio değerleme özetini hesaplar. */
  ExternalPortfolioSummaryResponse calculateSummary(UUID userId, Long portfolioId);

  /** Portfolio varlık dağılımını hesaplar. */
  List<ExternalPortfolioAllocationResponse> calculateAllocation(UUID userId, Long portfolioId);
}
