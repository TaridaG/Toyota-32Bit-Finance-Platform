package com.company.finance_api.portfolio.domain;

import java.math.BigDecimal;

/** Resolved FX pair for transaction history display (e.g. 1 USD = 40.37 TRY). */
public record FxDisplayLeg(String fromCurrency, String toCurrency, BigDecimal rate) {}
