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

  /** Trade execution öncesi maliyet, FX ve fiyat kapsamı önizlemesi döner. */
  TradePreviewResponse preview(TradeExecutionRequest request);

  /** Detaylı request ile alım (BUY) transaction'ı oluşturur. */
  Transaction buy(TradeExecutionRequest request);

  /** Detaylı request ile satım (SELL) transaction'ı oluşturur. */
  Transaction sell(TradeExecutionRequest request);

  /** Enstrüman için mevcut fiyat kapsamı ve valuation kaynak bilgisini döner. */
  InstrumentPriceCoverageResponse getPriceCoverage(Long instrumentId);
}
