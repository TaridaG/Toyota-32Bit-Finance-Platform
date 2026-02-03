package com.company.finance_api.service;

import com.company.finance_api.domain.Transaction;

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
}