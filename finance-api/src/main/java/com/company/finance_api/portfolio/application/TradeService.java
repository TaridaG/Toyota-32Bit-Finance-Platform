package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.pricing.infrastructure.http.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradeExecutionRequest;
import com.company.finance_api.portfolio.infrastructure.http.dto.TradePreviewResponse;
import java.math.BigDecimal;

/** TradeService iş mantığını uygular (trade service). */
public interface TradeService {

  Transaction buy(Long instrumentId, BigDecimal quantity);

  Transaction sell(Long instrumentId, BigDecimal quantity);

  /** preview sözleşmesi. */
  TradePreviewResponse preview(TradeExecutionRequest request);

  /** buy sözleşmesi. */
  Transaction buy(TradeExecutionRequest request);

  /** sell sözleşmesi. */
  Transaction sell(TradeExecutionRequest request);

  /** getPriceCoverage sözleşmesi. */
  InstrumentPriceCoverageResponse getPriceCoverage(Long instrumentId);
}
