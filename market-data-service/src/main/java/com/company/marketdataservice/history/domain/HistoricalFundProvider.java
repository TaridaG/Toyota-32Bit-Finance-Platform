package com.company.marketdataservice.history.domain;
import java.time.LocalDate;
import java.util.List;

/**
 * `geçmiş veri ve backfill` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
public interface HistoricalFundProvider {

    List<HistoricalFundPoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate);
}
