package com.company.finance_api.service;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.dto.TradeExecutionRequest;
import com.company.finance_api.dto.TradePreviewResponse;

import java.math.BigDecimal;

public interface TradeService {

    Transaction buy(
            Long instrumentId,
            BigDecimal quantity
    );

    Transaction sell(
            Long instrumentId,
            BigDecimal quantity
    );

    TradePreviewResponse preview(TradeExecutionRequest request);

    Transaction buy(TradeExecutionRequest request);

    Transaction sell(TradeExecutionRequest request);

    InstrumentPriceCoverageResponse getPriceCoverage(Long instrumentId);
}