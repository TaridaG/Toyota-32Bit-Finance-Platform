package com.company.marketdataservice.provider;

import java.math.BigDecimal;

public interface PriceProvider {
    String source();                 // "BINANCE"
    BigDecimal fetchPrice(String symbol);
}
