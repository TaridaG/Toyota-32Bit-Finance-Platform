package com.company.finance_api.portfolio.application;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.dto.TradeExecutionRequest;
import com.company.finance_api.dto.TradePreviewResponse;
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
