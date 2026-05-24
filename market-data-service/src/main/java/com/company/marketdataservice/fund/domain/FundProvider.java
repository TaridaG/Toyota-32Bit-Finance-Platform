package com.company.marketdataservice.fund.domain;
import java.util.List;

/**
 * `fon (TEFAS NAV)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
public interface FundProvider {

    String source();

    List<FundSnapshot> fetchLatestNavs(List<String> fundCodes);
}
