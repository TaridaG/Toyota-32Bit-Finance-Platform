package com.company.marketdataservice.spot.infrastructure.provider;
import java.math.BigDecimal;

/**
 * `spot fiyat` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
public interface PriceProvider {
    String source();                 // "BINANCE"
    BigDecimal fetchPrice(String symbol);
}
