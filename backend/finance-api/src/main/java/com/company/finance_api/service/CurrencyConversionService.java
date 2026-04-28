package com.company.finance_api.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface CurrencyConversionService {
    BigDecimal convert(BigDecimal price, String from, String to);
    String normalizeCurrency(String currency);
    Optional<BigDecimal> getRate(String symbol);
}
