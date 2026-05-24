package com.company.marketdataservice.fx.domain;
import java.util.List;

/**
 * `FX spot` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
public interface FxProvider {

    List<FxSnapshot> fetchLatestRates();

    String source();
}
