package com.company.finance_api.portfolio.valuation;

import java.math.BigDecimal;

public interface InstrumentPriceProvider {

    BigDecimal getCurrentPrice(Long instrumentId);
}

